package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pure calculation of the user's commitments for a horizon of {@code horizonDays} starting today
 * ({@code [today, today + horizonDays - 1]}, both inclusive).
 *
 * <ul>
 *   <li>{@code committed(o) = amount × (overdue + pending in the horizon)} for each ACTIVE obligation, including
 *       those whose account or category is inactive.</li>
 *   <li>{@code balance} is the sum of the ACTIVE accounts.</li>
 *   <li>{@code availableToSpend} and {@code shortfall} are compared first and subtracted in the non-negative
 *       direction, so money is never negative.</li>
 *   <li>Per-account shortfall only evaluates ACTIVE accounts, and only counts obligations whose account and
 *       category are active.</li>
 *   <li>Attention is ordered by {@link AttentionType} declaration order, then by the tie-breakers of each type.</li>
 * </ul>
 *
 * Inputs are snapshots, so read-side callers do not need to reconstruct aggregates.
 */
public final class CommitmentCalculator {
    public static final int DEFAULT_HORIZON_DAYS = 14;

    private final int horizonDays;

    public CommitmentCalculator(int horizonDays) {
        if (horizonDays <= 0) {
            throw new IllegalArgumentException("Horizon days must be greater than zero");
        }
        this.horizonDays = horizonDays;
    }

    /**
     * @param obligations the user's obligations; ARCHIVED ones do not contribute
     * @param resolutions the resolutions of those obligations
     * @param accounts every account referenced by an ACTIVE obligation, plus any other account of the user
     * @param categories every category referenced by an ACTIVE obligation
     */
    public CommitmentSummary calculate(LocalDate today, Collection<ObligationSnapshot> obligations,
                                       Collection<ResolutionSnapshot> resolutions,
                                       Collection<AccountSnapshot> accounts,
                                       Collection<CategorySnapshot> categories) {
        Objects.requireNonNull(today, "Today cannot be null");
        Objects.requireNonNull(obligations, "Obligations cannot be null");
        Objects.requireNonNull(resolutions, "Resolutions cannot be null");
        Objects.requireNonNull(accounts, "Accounts cannot be null");
        Objects.requireNonNull(categories, "Categories cannot be null");

        Map<UUID, AccountSnapshot> accountsById = new HashMap<>();
        accounts.forEach(account -> accountsById.put(account.id(), account));
        Map<UUID, CategorySnapshot> categoriesById = new HashMap<>();
        categories.forEach(category -> categoriesById.put(category.id(), category));
        Map<UUID, Set<LocalDate>> resolvedDatesByObligation = resolutions.stream()
            .collect(Collectors.groupingBy(ResolutionSnapshot::obligationId,
                Collectors.mapping(ResolutionSnapshot::dueDate, Collectors.toSet())));

        List<Evaluation> evaluations = new ArrayList<>();
        for (ObligationSnapshot obligation : obligations) {
            if (obligation.status() == ObligationStatus.ACTIVE) {
                evaluations.add(evaluate(obligation, today,
                    resolvedDatesByObligation.getOrDefault(obligation.id(), Set.of()), accountsById,
                    categoriesById));
            }
        }

        Money committedAmount = evaluations.stream()
            .map(evaluation -> evaluation.commitment().committed())
            .reduce(Money.ofCents(0), Money::add);
        Money balance = accounts.stream()
            .filter(account -> account.status() == AccountStatus.ACTIVE)
            .map(AccountSnapshot::balance)
            .reduce(Money.ofCents(0), Money::add);
        Money availableToSpend = nonNegativeDifference(balance, committedAmount);
        Money shortfall = nonNegativeDifference(committedAmount, balance);

        List<AccountEvaluation> accountEvaluations = accounts.stream()
            .filter(account -> account.status() == AccountStatus.ACTIVE)
            .map(account -> evaluateAccount(account, evaluations))
            .toList();

        List<Attention> attention = new ArrayList<>();
        attention.addAll(overdueAttention(evaluations));
        if (shortfall.isPositive()) {
            attention.add(new Attention.Shortfall(shortfall));
        }
        attention.addAll(paymentBlockedAttention(evaluations));
        attention.addAll(accountShortfallAttention(accountEvaluations));
        attention.sort(Comparator.comparing(Attention::type));

        return new CommitmentSummary(committedAmount, balance, availableToSpend, shortfall,
            evaluations.stream().map(Evaluation::commitment).toList(),
            accountEvaluations.stream().map(AccountEvaluation::commitment).toList(),
            attention);
    }

    private Evaluation evaluate(ObligationSnapshot obligation, LocalDate today, Set<LocalDate> resolvedDates,
                                Map<UUID, AccountSnapshot> accountsById,
                                Map<UUID, CategorySnapshot> categoriesById) {
        AccountSnapshot account = accountsById.get(obligation.accountId());
        if (account == null) {
            throw new IllegalArgumentException("Account of obligation is missing: " + obligation.accountId());
        }
        CategorySnapshot category = categoriesById.get(obligation.categoryId());
        if (category == null) {
            throw new IllegalArgumentException("Category of obligation is missing: " + obligation.categoryId());
        }

        Recurrence recurrence = obligation.recurrence();
        LocalDate yesterday = today.minusDays(1);
        long overdueCount = recurrence.countUnresolvedBetween(recurrence.startDate(), yesterday, resolvedDates);
        Optional<LocalDate> oldestOverdue = overdueCount > 0
            ? recurrence.firstDateBetweenExcluding(recurrence.startDate(), yesterday, resolvedDates)
            : Optional.empty();
        List<LocalDate> pendingDates = recurrence.unresolvedDatesBetween(today, today.plusDays(horizonDays - 1L),
            resolvedDates);
        Money overdueAmount = obligation.amount().multiply(overdueCount);
        Money committed = obligation.amount().multiply(overdueCount + pendingDates.size());
        boolean paymentSourceInactive = account.status() != AccountStatus.ACTIVE
            || category.status() != CategoryStatus.ACTIVE;
        Optional<LocalDate> nearestDueDate = oldestOverdue.or(() -> pendingDates.stream().findFirst());

        ObligationCommitment commitment = new ObligationCommitment(obligation.id(), committed, overdueCount,
            overdueAmount, pendingDates, paymentSourceInactive);
        return new Evaluation(obligation, commitment, oldestOverdue, nearestDueDate);
    }

    private static AccountEvaluation evaluateAccount(AccountSnapshot account, List<Evaluation> evaluations) {
        List<Evaluation> contributing = evaluations.stream()
            .filter(evaluation -> evaluation.obligation().accountId().equals(account.id()))
            .filter(evaluation -> !evaluation.commitment().paymentSourceInactive())
            .toList();
        Money committed = contributing.stream()
            .map(evaluation -> evaluation.commitment().committed())
            .reduce(Money.ofCents(0), Money::add);
        Money shortfall = nonNegativeDifference(committed, account.balance());
        Optional<LocalDate> nearestDueDate = contributing.stream()
            .map(Evaluation::nearestDueDate)
            .flatMap(Optional::stream)
            .min(Comparator.naturalOrder());
        return new AccountEvaluation(account,
            new AccountCommitment(account.id(), committed, account.balance(), shortfall), nearestDueDate);
    }

    private static List<Attention> overdueAttention(List<Evaluation> evaluations) {
        return evaluations.stream()
            .filter(evaluation -> evaluation.oldestOverdue().isPresent())
            .sorted(Comparator.comparing((Evaluation evaluation) -> evaluation.oldestOverdue().orElseThrow())
                .thenComparing(evaluation -> evaluation.commitment().overdueAmount(), CommitmentCalculator::descending)
                .thenComparing(evaluation -> evaluation.obligation().name())
                .thenComparing(evaluation -> evaluation.obligation().id()))
            .<Attention>map(evaluation -> new Attention.OverdueOccurrence(evaluation.obligation().id(),
                evaluation.oldestOverdue().orElseThrow(), evaluation.commitment().overdueCount(),
                evaluation.commitment().overdueAmount()))
            .toList();
    }

    private static List<Attention> paymentBlockedAttention(List<Evaluation> evaluations) {
        return evaluations.stream()
            .filter(evaluation -> evaluation.commitment().paymentSourceInactive())
            .filter(evaluation -> evaluation.nearestDueDate().isPresent())
            .sorted(Comparator.comparing((Evaluation evaluation) -> evaluation.nearestDueDate().orElseThrow())
                .thenComparing(evaluation -> evaluation.commitment().committed(), CommitmentCalculator::descending)
                .thenComparing(evaluation -> evaluation.obligation().name())
                .thenComparing(evaluation -> evaluation.obligation().id()))
            .<Attention>map(evaluation -> new Attention.PaymentBlocked(evaluation.obligation().id(),
                evaluation.nearestDueDate().orElseThrow(), evaluation.commitment().committed()))
            .toList();
    }

    private static List<Attention> accountShortfallAttention(List<AccountEvaluation> accountEvaluations) {
        return accountEvaluations.stream()
            .filter(evaluation -> evaluation.commitment().shortfall().isPositive())
            .sorted(Comparator.comparing((AccountEvaluation evaluation) -> evaluation.commitment().shortfall(),
                    CommitmentCalculator::descending)
                .thenComparing(evaluation -> evaluation.nearestDueDate().orElseThrow())
                .thenComparing(evaluation -> evaluation.account().name())
                .thenComparing(evaluation -> evaluation.account().id()))
            .<Attention>map(evaluation -> new Attention.AccountShortfall(evaluation.account().id(),
                evaluation.commitment().shortfall(), evaluation.nearestDueDate().orElseThrow()))
            .toList();
    }

    private static Money nonNegativeDifference(Money minuend, Money subtrahend) {
        if (minuend.compareTo(subtrahend) >= 0) {
            return minuend.subtract(subtrahend);
        }
        return Money.ofCents(0);
    }

    private static int descending(Money first, Money second) {
        return second.compareTo(first);
    }

    private record Evaluation(ObligationSnapshot obligation, ObligationCommitment commitment,
                              Optional<LocalDate> oldestOverdue, Optional<LocalDate> nearestDueDate) {
    }

    private record AccountEvaluation(AccountSnapshot account, AccountCommitment commitment,
                                     Optional<LocalDate> nearestDueDate) {
    }
}
