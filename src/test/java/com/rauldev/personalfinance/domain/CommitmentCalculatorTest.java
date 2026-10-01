package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CommitmentCalculatorTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final Instant RESOLVED_AT = Instant.parse("2026-08-20T12:00:00Z");

    private final UUID userId = UUID.randomUUID();
    private final CommitmentCalculator calculator = new CommitmentCalculator(CommitmentCalculator.DEFAULT_HORIZON_DAYS);

    private static LocalDate d(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    private static UUID uuid(int n) {
        return new UUID(0L, n);
    }

    private Account account(String name, long balanceCents, boolean active) {
        return accountWithId(UUID.randomUUID(), name, balanceCents, active);
    }

    private Account accountWithId(UUID id, String name, long balanceCents, boolean active) {
        Account account = new Account(id, userId, name);
        if (balanceCents > 0) {
            account.credit(Money.ofCents(balanceCents));
        }
        if (!active) {
            account.deactivate();
        }
        return account;
    }

    private Category category(boolean active) {
        Category category = new Category(userId, "Category", CategoryType.EXPENSE);
        if (!active) {
            category.deactivate();
        }
        return category;
    }

    private static Recurrence once(LocalDate date) {
        return new Recurrence(Frequency.ONCE, date, null);
    }

    private static Recurrence weekly(LocalDate start) {
        return new Recurrence(Frequency.WEEKLY, start, null);
    }

    private Obligation obligation(String name, long cents, Account account, Category category,
                                  Recurrence recurrence) {
        return obligationWithId(UUID.randomUUID(), name, cents, account, category, recurrence,
            ObligationStatus.ACTIVE);
    }

    private Obligation obligationWithId(UUID id, String name, long cents, Account account, Category category,
                                        Recurrence recurrence, ObligationStatus status) {
        return new Obligation(id, userId, name, Money.ofCents(cents), account.id(), category.id(), recurrence,
            status);
    }

    private static OccurrenceResolution skipped(Obligation obligation, LocalDate dueDate) {
        return new OccurrenceResolution(UUID.randomUUID(), obligation.id(), dueDate, ResolutionStatus.SKIPPED, null,
            RESOLVED_AT);
    }

    private static OccurrenceResolution paid(Obligation obligation, LocalDate dueDate) {
        return new OccurrenceResolution(UUID.randomUUID(), obligation.id(), dueDate, ResolutionStatus.PAID,
            UUID.randomUUID(), RESOLVED_AT);
    }

    private static ObligationSnapshot snapshot(Obligation obligation) {
        return new ObligationSnapshot(obligation.id(), obligation.name(), obligation.amount(),
            obligation.accountId(), obligation.categoryId(), obligation.recurrence(), obligation.status());
    }

    private static ResolutionSnapshot snapshot(OccurrenceResolution resolution) {
        return new ResolutionSnapshot(resolution.obligationId(), resolution.dueDate());
    }

    private static AccountSnapshot snapshot(Account account) {
        return new AccountSnapshot(account.id(), account.name(), account.balance(), account.status());
    }

    private static CategorySnapshot snapshot(Category category) {
        return new CategorySnapshot(category.id(), category.status());
    }

    private CommitmentSummary calculateAt(CommitmentCalculator calculator, LocalDate today,
                                          List<Obligation> obligations, List<OccurrenceResolution> resolutions,
                                          List<Account> accounts, List<Category> categories) {
        return calculator.calculate(today, obligations.stream().map(CommitmentCalculatorTest::snapshot).toList(),
            resolutions.stream().map(CommitmentCalculatorTest::snapshot).toList(),
            accounts.stream().map(CommitmentCalculatorTest::snapshot).toList(),
            categories.stream().map(CommitmentCalculatorTest::snapshot).toList());
    }

    private CommitmentSummary calculate(List<Obligation> obligations, List<OccurrenceResolution> resolutions,
                                        List<Account> accounts, List<Category> categories) {
        return calculateAt(calculator, TODAY, obligations, resolutions, accounts, categories);
    }

    private static ObligationCommitment commitmentOf(CommitmentSummary summary, Obligation obligation) {
        return summary.obligations().stream()
            .filter(commitment -> commitment.obligationId().equals(obligation.id()))
            .findFirst()
            .orElseThrow();
    }

    private static List<Attention> attentionOfType(CommitmentSummary summary, AttentionType type) {
        return summary.attention().stream().filter(attention -> attention.type() == type).toList();
    }

    // Construction

    @Test
    void rejectsZeroHorizon() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new CommitmentCalculator(0));

        assertEquals("Horizon days must be greater than zero", exception.getMessage());
    }

    @Test
    void rejectsNegativeHorizon() {
        assertThrows(IllegalArgumentException.class, () -> new CommitmentCalculator(-1));
    }

    @Test
    void calculateRejectsNullArguments() {
        assertThrows(NullPointerException.class,
            () -> calculator.calculate(null, List.of(), List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class,
            () -> calculator.calculate(TODAY, null, List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class,
            () -> calculator.calculate(TODAY, List.of(), null, List.of(), List.of()));
        assertThrows(NullPointerException.class,
            () -> calculator.calculate(TODAY, List.of(), List.of(), null, List.of()));
        assertThrows(NullPointerException.class,
            () -> calculator.calculate(TODAY, List.of(), List.of(), List.of(), null));
    }

    @Test
    void emptyInputProducesAnEmptyZeroSummary() {
        CommitmentSummary summary = calculate(List.of(), List.of(), List.of(), List.of());

        assertEquals(Money.ofCents(0), summary.committedAmount());
        assertEquals(Money.ofCents(0), summary.balance());
        assertEquals(Money.ofCents(0), summary.availableToSpend());
        assertEquals(Money.ofCents(0), summary.shortfall());
        assertEquals(List.of(), summary.obligations());
        assertEquals(List.of(), summary.accounts());
        assertEquals(List.of(), summary.attention());
    }

    // Horizon

    @Test
    void horizonIncludesTodayAndTodayPlus13ButNotTodayPlus14() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation onToday = obligation("A", 100, account, category, once(TODAY));
        Obligation onLastDay = obligation("B", 100, account, category, once(TODAY.plusDays(13)));
        Obligation afterHorizon = obligation("C", 100, account, category, once(TODAY.plusDays(14)));

        CommitmentSummary summary = calculate(List.of(onToday, onLastDay, afterHorizon), List.of(),
            List.of(account), List.of(category));

        assertEquals(List.of(TODAY), commitmentOf(summary, onToday).pendingDates());
        assertEquals(List.of(d(2026, 9, 2)), commitmentOf(summary, onLastDay).pendingDates());
        assertEquals(List.of(), commitmentOf(summary, afterHorizon).pendingDates());
        assertEquals(Money.ofCents(200), summary.committedAmount());
    }

    @Test
    void customHorizonOfOneDayOnlyIncludesToday() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, weekly(TODAY));

        CommitmentSummary summary = calculateAt(new CommitmentCalculator(1), TODAY, List.of(obligation), List.of(),
            List.of(account), List.of(category));

        assertEquals(List.of(TODAY), commitmentOf(summary, obligation).pendingDates());
    }

    @Test
    void pendingDatesListEveryUnresolvedDateInTheHorizon() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, weekly(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(account), List.of(category));

        assertEquals(List.of(TODAY, d(2026, 8, 27)), commitmentOf(summary, obligation).pendingDates());
        assertEquals(Money.ofCents(200), commitmentOf(summary, obligation).committed());
    }

    @Test
    void resolvedPendingOccurrencesAreExcluded() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, weekly(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(paid(obligation, TODAY)),
            List.of(account), List.of(category));

        assertEquals(List.of(d(2026, 8, 27)), commitmentOf(summary, obligation).pendingDates());
        assertEquals(Money.ofCents(100), summary.committedAmount());
    }

    // Overdue and committed

    @Test
    void overdueAmountAndCommittedAreAmountTimesOccurrences() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, weekly(d(2026, 8, 6)));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(account), List.of(category));

        ObligationCommitment commitment = commitmentOf(summary, obligation);
        assertEquals(2, commitment.overdueCount());
        assertEquals(Money.ofCents(200), commitment.overdueAmount());
        assertEquals(List.of(TODAY, d(2026, 8, 27)), commitment.pendingDates());
        assertEquals(Money.ofCents(400), commitment.committed());
        assertEquals(Money.ofCents(400), summary.committedAmount());
    }

    @Test
    void resolvedPastOccurrencesAreNotCountedAsOverdue() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, weekly(d(2026, 8, 6)));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(paid(obligation, d(2026, 8, 6))),
            List.of(account), List.of(category));

        ObligationCommitment commitment = commitmentOf(summary, obligation);
        assertEquals(1, commitment.overdueCount());
        assertEquals(Money.ofCents(100), commitment.overdueAmount());
        assertEquals(Money.ofCents(300), commitment.committed());
        assertEquals(Optional.of(d(2026, 8, 13)),
            Optional.of(((Attention.OverdueOccurrence) attentionOfType(summary, AttentionType.OVERDUE_OCCURRENCE)
                .get(0)).oldestOverdue()));
    }

    @Test
    void archivedObligationsContributeNothingAndAreAbsent() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation archived = obligationWithId(UUID.randomUUID(), "A", 100, account, category,
            weekly(d(2026, 8, 6)), ObligationStatus.ARCHIVED);

        CommitmentSummary summary = calculate(List.of(archived), List.of(), List.of(account), List.of(category));

        assertEquals(List.of(), summary.obligations());
        assertEquals(Money.ofCents(0), summary.committedAmount());
        assertEquals(List.of(), summary.attention());
    }

    @Test
    void archivedObligationDoesNotRequireItsAccountOrCategory() {
        Account missingAccount = account("Gone", 0, true);
        Category missingCategory = category(true);
        Obligation archived = obligationWithId(UUID.randomUUID(), "A", 100, missingAccount, missingCategory,
            weekly(d(2026, 8, 6)), ObligationStatus.ARCHIVED);

        CommitmentSummary summary = calculate(List.of(archived), List.of(), List.of(), List.of());

        assertEquals(List.of(), summary.obligations());
    }

    @Test
    void futureStartContributesNothing() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, weekly(TODAY.plusDays(20)));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(account), List.of(category));

        ObligationCommitment commitment = commitmentOf(summary, obligation);
        assertEquals(0, commitment.overdueCount());
        assertEquals(List.of(), commitment.pendingDates());
        assertEquals(Money.ofCents(0), commitment.committed());
    }

    @Test
    void endedAndFullyResolvedObligationContributesNothing() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category,
            new Recurrence(Frequency.WEEKLY, d(2026, 7, 1), d(2026, 7, 15)));
        List<OccurrenceResolution> resolutions = List.of(paid(obligation, d(2026, 7, 1)),
            skipped(obligation, d(2026, 7, 8)), paid(obligation, d(2026, 7, 15)));

        CommitmentSummary summary = calculate(List.of(obligation), resolutions, List.of(account),
            List.of(category));

        assertEquals(Money.ofCents(0), commitmentOf(summary, obligation).committed());
        assertEquals(List.of(), summary.attention());
    }

    @Test
    void endedObligationWithUnresolvedDatesStillCountsThemAsOverdue() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category,
            new Recurrence(Frequency.WEEKLY, d(2026, 7, 1), d(2026, 7, 15)));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(account), List.of(category));

        assertEquals(3, commitmentOf(summary, obligation).overdueCount());
        assertEquals(List.of(), commitmentOf(summary, obligation).pendingDates());
    }

    @Test
    void committedAmountIncludesObligationsWithInactivePaymentSource() {
        Account inactive = account("Old", 0, false);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, inactive, category, once(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(inactive),
            List.of(category));

        assertEquals(Money.ofCents(100), summary.committedAmount());
        assertTrue(commitmentOf(summary, obligation).paymentSourceInactive());
    }

    // Balance, available and shortfall

    @Test
    void balanceSumsOnlyActiveAccounts() {
        Account first = account("First", 1000, true);
        Account second = account("Second", 250, true);
        Account inactive = account("Old", 9999, false);

        CommitmentSummary summary = calculate(List.of(), List.of(), List.of(first, second, inactive), List.of());

        assertEquals(Money.ofCents(1250), summary.balance());
    }

    @Test
    void balanceGreaterThanCommittedLeavesAvailableAndNoShortfall() {
        Account account = account("Checking", 300, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, once(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(account), List.of(category));

        assertEquals(Money.ofCents(200), summary.availableToSpend());
        assertEquals(Money.ofCents(0), summary.shortfall());
        assertEquals(List.of(), attentionOfType(summary, AttentionType.SHORTFALL));
    }

    @Test
    void balanceLowerThanCommittedLeavesShortfallAndNoAvailable() {
        Account account = account("Checking", 100, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 300, account, category, once(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(account), List.of(category));

        assertEquals(Money.ofCents(0), summary.availableToSpend());
        assertEquals(Money.ofCents(200), summary.shortfall());
        assertEquals(List.of(new Attention.Shortfall(Money.ofCents(200))),
            attentionOfType(summary, AttentionType.SHORTFALL));
    }

    @Test
    void balanceEqualToCommittedLeavesBothZeroAndNoAttention() {
        Account account = account("Checking", 300, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 300, account, category, once(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(account), List.of(category));

        assertEquals(Money.ofCents(0), summary.availableToSpend());
        assertEquals(Money.ofCents(0), summary.shortfall());
        assertEquals(List.of(), summary.attention());
    }

    // PAYMENT_BLOCKED

    @Test
    void inactiveAccountBlocksPaymentWhenSomethingIsPending() {
        Account inactive = account("Old", 0, false);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, inactive, category, once(d(2026, 8, 22)));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(inactive),
            List.of(category));

        assertEquals(List.of(new Attention.PaymentBlocked(obligation.id(), d(2026, 8, 22), Money.ofCents(100))),
            attentionOfType(summary, AttentionType.PAYMENT_BLOCKED));
    }

    @Test
    void inactiveCategoryBlocksPaymentWhenSomethingIsPending() {
        Account account = account("Checking", 1000, true);
        Category inactive = category(false);
        Obligation obligation = obligation("A", 100, account, inactive, once(d(2026, 8, 22)));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(account),
            List.of(inactive));

        assertEquals(List.of(new Attention.PaymentBlocked(obligation.id(), d(2026, 8, 22), Money.ofCents(100))),
            attentionOfType(summary, AttentionType.PAYMENT_BLOCKED));
        assertTrue(commitmentOf(summary, obligation).paymentSourceInactive());
    }

    @Test
    void paymentIsNotBlockedWhenNothingIsDue() {
        Account inactive = account("Old", 0, false);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, inactive, category, once(TODAY.plusDays(14)));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(inactive),
            List.of(category));

        assertEquals(List.of(), attentionOfType(summary, AttentionType.PAYMENT_BLOCKED));
        assertTrue(commitmentOf(summary, obligation).paymentSourceInactive());
    }

    @Test
    void paymentIsNotBlockedWhenEverythingDueIsResolved() {
        Account inactive = account("Old", 0, false);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, inactive, category, once(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(skipped(obligation, TODAY)),
            List.of(inactive), List.of(category));

        assertEquals(List.of(), attentionOfType(summary, AttentionType.PAYMENT_BLOCKED));
    }

    @Test
    void overdueAndBlockedObligationAppearsInBothAttentionsWithTheOldestOverdueAsNearestDate() {
        Account inactive = account("Old", 0, false);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, inactive, category, weekly(d(2026, 8, 13)));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(inactive),
            List.of(category));

        assertEquals(List.of(new Attention.OverdueOccurrence(obligation.id(), d(2026, 8, 13), 1,
            Money.ofCents(100))), attentionOfType(summary, AttentionType.OVERDUE_OCCURRENCE));
        assertEquals(List.of(new Attention.PaymentBlocked(obligation.id(), d(2026, 8, 13), Money.ofCents(300))),
            attentionOfType(summary, AttentionType.PAYMENT_BLOCKED));
    }

    // ACCOUNT_SHORTFALL

    @Test
    void accountShortfallIsCommittedMinusBalanceWithNearestDate() {
        Account account = account("Checking", 100, true);
        Category category = category(true);
        Obligation first = obligation("A", 100, account, category, once(TODAY));
        Obligation second = obligation("B", 150, account, category, once(d(2026, 8, 25)));

        CommitmentSummary summary = calculate(List.of(second, first), List.of(), List.of(account),
            List.of(category));

        assertEquals(List.of(new AccountCommitment(account.id(), Money.ofCents(250), Money.ofCents(100),
            Money.ofCents(150))), summary.accounts());
        assertEquals(List.of(new Attention.AccountShortfall(account.id(), Money.ofCents(150), TODAY)),
            attentionOfType(summary, AttentionType.ACCOUNT_SHORTFALL));
    }

    @Test
    void accountShortfallCanExistEvenWhenTheGlobalBalanceCoversTheCommitments() {
        Account poor = account("Poor", 0, true);
        Account rich = account("Rich", 1000, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, poor, category, once(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(poor, rich),
            List.of(category));

        assertEquals(Money.ofCents(0), summary.shortfall());
        assertEquals(List.of(new Attention.AccountShortfall(poor.id(), Money.ofCents(100), TODAY)),
            attentionOfType(summary, AttentionType.ACCOUNT_SHORTFALL));
    }

    @Test
    void accountHasNoShortfallWhenBalanceEqualsOrExceedsItsCommitments() {
        Account equal = account("Equal", 100, true);
        Account greater = account("Greater", 500, true);
        Category category = category(true);
        Obligation first = obligation("A", 100, equal, category, once(TODAY));
        Obligation second = obligation("B", 100, greater, category, once(TODAY));

        CommitmentSummary summary = calculate(List.of(first, second), List.of(), List.of(equal, greater),
            List.of(category));

        assertEquals(List.of(), attentionOfType(summary, AttentionType.ACCOUNT_SHORTFALL));
        assertEquals(Money.ofCents(0), summary.accounts().get(0).shortfall());
        assertEquals(Money.ofCents(0), summary.accounts().get(1).shortfall());
    }

    @Test
    void accountShortfallExcludesObligationsWithInactiveCategory() {
        Account account = account("Checking", 0, true);
        Category inactive = category(false);
        Obligation obligation = obligation("A", 100, account, inactive, once(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(account),
            List.of(inactive));

        assertEquals(Money.ofCents(100), summary.committedAmount());
        assertEquals(Money.ofCents(0), summary.accounts().get(0).committed());
        assertEquals(List.of(), attentionOfType(summary, AttentionType.ACCOUNT_SHORTFALL));
    }

    @Test
    void inactiveAccountsAreAbsentFromTheAccountsList() {
        Account active = account("Active", 0, true);
        Account inactive = account("Old", 0, false);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, inactive, category, once(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(active, inactive),
            List.of(category));

        assertEquals(1, summary.accounts().size());
        assertEquals(active.id(), summary.accounts().get(0).accountId());
        assertEquals(List.of(), attentionOfType(summary, AttentionType.ACCOUNT_SHORTFALL));
    }

    // Missing references

    @Test
    void activeObligationWithMissingAccountIsRejected() {
        Account missing = account("Gone", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, missing, category, once(TODAY));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> calculate(List.of(obligation), List.of(), List.of(), List.of(category)));

        assertEquals("Account of obligation is missing: " + missing.id(), exception.getMessage());
    }

    @Test
    void activeObligationWithMissingCategoryIsRejected() {
        Account account = account("Checking", 0, true);
        Category missing = category(true);
        Obligation obligation = obligation("A", 100, account, missing, once(TODAY));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> calculate(List.of(obligation), List.of(), List.of(account), List.of()));

        assertEquals("Category of obligation is missing: " + missing.id(), exception.getMessage());
    }

    // Re-anchor

    @Test
    void offCalendarResolutionDoesNotReduceOverdue() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, weekly(TODAY));
        OccurrenceResolution paidAhead = paid(obligation, d(2026, 8, 25));
        LocalDate later = d(2026, 9, 1);

        CommitmentSummary summary = calculateAt(calculator, later, List.of(obligation), List.of(paidAhead),
            List.of(account), List.of(category));

        ObligationCommitment commitment = commitmentOf(summary, obligation);
        assertEquals(2, commitment.overdueCount());
        assertEquals(List.of(d(2026, 9, 3), d(2026, 9, 10)), commitment.pendingDates());
        assertEquals(Money.ofCents(400), commitment.committed());
    }

    // Attention ordering

    @Test
    void attentionIsOrderedByType() {
        Account active = account("Active", 0, true);
        Account inactive = account("Old", 0, false);
        Category category = category(true);
        Obligation overdue = obligation("Overdue", 100, active, category, weekly(d(2026, 8, 13)));
        Obligation blocked = obligation("Blocked", 50, inactive, category, once(d(2026, 8, 21)));

        CommitmentSummary summary = calculate(List.of(blocked, overdue), List.of(), List.of(inactive, active),
            List.of(category));

        assertEquals(List.of(AttentionType.OVERDUE_OCCURRENCE, AttentionType.SHORTFALL,
            AttentionType.PAYMENT_BLOCKED, AttentionType.ACCOUNT_SHORTFALL),
            summary.attention().stream().map(Attention::type).toList());
    }

    @Test
    void overdueAttentionIsOrderedByOldestDateThenAmountDescThenNameThenId() {
        Account account = account("Checking", 1_000_000, true);
        Category category = category(true);
        Obligation oldest = obligationWithId(uuid(9), "Oldest", 100, account, category, weekly(d(2026, 8, 10)),
            ObligationStatus.ACTIVE);
        Obligation biggest = obligationWithId(uuid(8), "Biggest", 900, account, category, once(d(2026, 8, 15)),
            ObligationStatus.ACTIVE);
        Obligation alpha = obligationWithId(uuid(7), "Alpha", 500, account, category, once(d(2026, 8, 15)),
            ObligationStatus.ACTIVE);
        Obligation betaSecond = obligationWithId(uuid(2), "Beta", 500, account, category, once(d(2026, 8, 15)),
            ObligationStatus.ACTIVE);
        Obligation betaFirst = obligationWithId(uuid(1), "Beta", 500, account, category, once(d(2026, 8, 15)),
            ObligationStatus.ACTIVE);

        CommitmentSummary summary = calculate(List.of(betaSecond, alpha, oldest, betaFirst, biggest), List.of(),
            List.of(account), List.of(category));

        assertEquals(List.of(oldest.id(), biggest.id(), alpha.id(), betaFirst.id(), betaSecond.id()),
            attentionOfType(summary, AttentionType.OVERDUE_OCCURRENCE).stream()
                .map(attention -> ((Attention.OverdueOccurrence) attention).obligationId()).toList());
        assertEquals(new Attention.OverdueOccurrence(oldest.id(), d(2026, 8, 10), 2, Money.ofCents(200)),
            summary.attention().get(0));
    }

    @Test
    void paymentBlockedAttentionIsOrderedByNearestDateThenCommittedDescThenNameThenId() {
        Account inactive = account("Old", 0, false);
        Category category = category(true);
        Obligation earliest = obligationWithId(uuid(9), "Q", 100, inactive, category, once(d(2026, 8, 21)),
            ObligationStatus.ACTIVE);
        Obligation biggest = obligationWithId(uuid(8), "R", 300, inactive, category, once(d(2026, 8, 22)),
            ObligationStatus.ACTIVE);
        Obligation alpha = obligationWithId(uuid(7), "Alpha", 100, inactive, category, once(d(2026, 8, 22)),
            ObligationStatus.ACTIVE);
        Obligation pee = obligationWithId(uuid(6), "P", 100, inactive, category, once(d(2026, 8, 22)),
            ObligationStatus.ACTIVE);
        Obligation teeSecond = obligationWithId(uuid(2), "T", 100, inactive, category, once(d(2026, 8, 22)),
            ObligationStatus.ACTIVE);
        Obligation teeFirst = obligationWithId(uuid(1), "T", 100, inactive, category, once(d(2026, 8, 22)),
            ObligationStatus.ACTIVE);

        CommitmentSummary summary = calculate(List.of(teeSecond, pee, biggest, teeFirst, earliest, alpha),
            List.of(), List.of(inactive), List.of(category));

        assertEquals(List.of(earliest.id(), biggest.id(), alpha.id(), pee.id(), teeFirst.id(), teeSecond.id()),
            attentionOfType(summary, AttentionType.PAYMENT_BLOCKED).stream()
                .map(attention -> ((Attention.PaymentBlocked) attention).obligationId()).toList());
    }

    @Test
    void accountShortfallAttentionIsOrderedByShortfallDescThenNearestDateThenNameThenId() {
        Category category = category(true);
        Account big = accountWithId(uuid(100), "Big", 0, true);
        Account zeta = accountWithId(uuid(101), "Zeta", 0, true);
        Account charlie = accountWithId(uuid(102), "Charlie", 0, true);
        Account bravo = accountWithId(uuid(103), "Bravo", 0, true);
        Account deltaSecond = accountWithId(uuid(105), "Delta", 0, true);
        Account deltaFirst = accountWithId(uuid(104), "Delta", 0, true);
        List<Obligation> obligations = List.of(
            obligation("o1", 900, big, category, once(d(2026, 8, 30))),
            obligation("o2", 500, zeta, category, once(d(2026, 8, 21))),
            obligation("o3", 500, charlie, category, once(d(2026, 8, 25))),
            obligation("o4", 500, bravo, category, once(d(2026, 8, 25))),
            obligation("o5", 500, deltaSecond, category, once(d(2026, 8, 25))),
            obligation("o6", 500, deltaFirst, category, once(d(2026, 8, 25))));

        CommitmentSummary summary = calculate(obligations, List.of(),
            List.of(deltaSecond, charlie, big, deltaFirst, zeta, bravo), List.of(category));

        assertEquals(List.of(big.id(), zeta.id(), bravo.id(), charlie.id(), deltaFirst.id(), deltaSecond.id()),
            attentionOfType(summary, AttentionType.ACCOUNT_SHORTFALL).stream()
                .map(attention -> ((Attention.AccountShortfall) attention).accountId()).toList());
    }

    @Test
    void oldestOverdueSkipsAResolvedEarliestDate() {
        Account account = account("Checking", 1_000_000, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, weekly(d(2026, 8, 6)));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(paid(obligation, d(2026, 8, 6))),
            List.of(account), List.of(category));

        assertEquals(List.of(new Attention.OverdueOccurrence(obligation.id(), d(2026, 8, 13), 1,
            Money.ofCents(100))), attentionOfType(summary, AttentionType.OVERDUE_OCCURRENCE));
    }

    // Snapshots, duplicates and missing attention types

    @Test
    void duplicateResolutionSnapshotsForTheSameDateCountOnce() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, weekly(d(2026, 8, 13)));
        ResolutionSnapshot resolution = new ResolutionSnapshot(obligation.id(), d(2026, 8, 13));

        CommitmentSummary summary = calculator.calculate(TODAY, List.of(snapshot(obligation)),
            List.of(resolution, resolution, new ResolutionSnapshot(obligation.id(), d(2026, 8, 13))),
            List.of(snapshot(account)), List.of(snapshot(category)));

        ObligationCommitment commitment = commitmentOf(summary, obligation);
        assertEquals(0, commitment.overdueCount());
        assertEquals(List.of(TODAY, d(2026, 8, 27)), commitment.pendingDates());
        assertEquals(Money.ofCents(200), commitment.committed());
    }

    @Test
    void resolutionSnapshotsOfUnknownObligationsAreIgnored() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, once(TODAY));

        CommitmentSummary summary = calculator.calculate(TODAY, List.of(snapshot(obligation)),
            List.of(new ResolutionSnapshot(UUID.randomUUID(), TODAY)), List.of(snapshot(account)),
            List.of(snapshot(category)));

        assertEquals(List.of(TODAY), commitmentOf(summary, obligation).pendingDates());
        assertEquals(Money.ofCents(100), summary.committedAmount());
    }

    @Test
    void attentionKeepsTypeOrderWhenShortfallIsMissing() {
        Account funded = account("Funded", 1_000_000, true);
        Account poor = account("Poor", 0, true);
        Account inactive = account("Old", 0, false);
        Category category = category(true);
        Obligation overdue = obligation("Overdue", 100, funded, category, once(d(2026, 8, 13)));
        Obligation blocked = obligation("Blocked", 50, inactive, category, once(d(2026, 8, 21)));
        Obligation short_ = obligation("Short", 100, poor, category, once(d(2026, 8, 22)));

        CommitmentSummary summary = calculate(List.of(short_, blocked, overdue), List.of(),
            List.of(poor, inactive, funded), List.of(category));

        assertEquals(Money.ofCents(0), summary.shortfall());
        assertEquals(List.of(AttentionType.OVERDUE_OCCURRENCE, AttentionType.PAYMENT_BLOCKED,
            AttentionType.ACCOUNT_SHORTFALL), summary.attention().stream().map(Attention::type).toList());
    }

    // Overdue-driven account shortfall and inactive-only global shortfall

    @Test
    void accountShortfallDrivenByOverdueUsesTheOldestOverdueAsNearestDate() {
        Account account = account("Checking", 100, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, weekly(d(2026, 8, 6)));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(account), List.of(category));

        assertEquals(List.of(new Attention.AccountShortfall(account.id(), Money.ofCents(300), d(2026, 8, 6))),
            attentionOfType(summary, AttentionType.ACCOUNT_SHORTFALL));
    }

    @Test
    void globalShortfallWhenTheOnlyObligationAccountIsInactive() {
        Account inactive = account("Old", 5000, false);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, inactive, category, once(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(inactive),
            List.of(category));

        assertEquals(Money.ofCents(100), summary.committedAmount());
        assertEquals(Money.ofCents(0), summary.balance());
        assertEquals(Money.ofCents(100), summary.shortfall());
        assertEquals(Money.ofCents(0), summary.availableToSpend());
        assertEquals(List.of(new Attention.Shortfall(Money.ofCents(100))),
            attentionOfType(summary, AttentionType.SHORTFALL));
    }

    // Immutability

    @Test
    void resultListsAreImmutable() {
        Account account = account("Checking", 0, true);
        Category category = category(true);
        Obligation obligation = obligation("A", 100, account, category, once(TODAY));

        CommitmentSummary summary = calculate(List.of(obligation), List.of(), List.of(account), List.of(category));

        assertThrows(UnsupportedOperationException.class, () -> summary.obligations().clear());
        assertThrows(UnsupportedOperationException.class, () -> summary.accounts().clear());
        assertThrows(UnsupportedOperationException.class, () -> summary.attention().clear());
        assertThrows(UnsupportedOperationException.class,
            () -> summary.obligations().get(0).pendingDates().clear());
    }
}
