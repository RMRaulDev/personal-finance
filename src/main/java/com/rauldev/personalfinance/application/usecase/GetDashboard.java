package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.rauldev.personalfinance.application.port.out.DashboardQueryPort;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.AttentionItem;
import com.rauldev.personalfinance.application.readmodel.AvailableToSpend;
import com.rauldev.personalfinance.application.readmodel.Dashboard;
import com.rauldev.personalfinance.application.readmodel.DashboardHorizon;
import com.rauldev.personalfinance.application.readmodel.ObligationSummary;
import com.rauldev.personalfinance.application.readmodel.RecentActivityItem;
import com.rauldev.personalfinance.application.readmodel.UpcomingCommitment;
import com.rauldev.personalfinance.domain.AccountSnapshot;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.Attention;
import com.rauldev.personalfinance.domain.CategorySnapshot;
import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.CommitmentCalculator;
import com.rauldev.personalfinance.domain.CommitmentSummary;
import com.rauldev.personalfinance.domain.ObligationCommitment;
import com.rauldev.personalfinance.domain.ObligationSnapshot;
import com.rauldev.personalfinance.domain.ResolutionSnapshot;

/**
 * Builds the user's dashboard with {@link CommitmentCalculator}.
 *
 * <p>Unlike the other query use cases, all reads run in one transaction so that they share a single connection and
 * a consistent snapshot of the database.
 *
 * <p>A missing account or category of an active obligation can only come from corrupt or cross-user data, so the
 * calculator's {@link IllegalArgumentException} is rethrown as an {@link IllegalStateException} (a server fault).
 */
public final class GetDashboard {
    private static final int HORIZON_DAYS = CommitmentCalculator.DEFAULT_HORIZON_DAYS;
    private static final int UPCOMING_COMMITMENTS_LIMIT = 5;
    private static final int RECENT_ACTIVITY_LIMIT = 5;

    private final DashboardQueryPort dashboardQueryPort;
    private final TransactionManager transactionManager;
    private final Clock clock;
    private final CommitmentCalculator commitmentCalculator = new CommitmentCalculator(HORIZON_DAYS);

    public GetDashboard(
        DashboardQueryPort dashboardQueryPort,
        TransactionManager transactionManager,
        Clock clock
    ) {
        this.dashboardQueryPort = Objects.requireNonNull(dashboardQueryPort, "Dashboard query port cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    public Dashboard execute(GetDashboardQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");

        return transactionManager.execute(() -> {
            LocalDate today = LocalDate.now(clock);
            UUID userId = query.userId();

            List<ObligationSnapshot> obligations = dashboardQueryPort.findActiveObligations(userId);
            List<ResolutionSnapshot> resolutions = dashboardQueryPort.findResolutionsOfActiveObligations(userId);
            List<AccountSnapshot> accounts = dashboardQueryPort.findAccounts(userId);
            List<CategorySnapshot> categories = dashboardQueryPort.findCategories(userId);
            List<RecentActivityItem> recent = dashboardQueryPort.findRecentActivity(userId, RECENT_ACTIVITY_LIMIT);

            CommitmentSummary summary = calculate(today, obligations, resolutions, accounts, categories);

            Map<UUID, ObligationSnapshot> obligationsById = indexById(obligations, ObligationSnapshot::id);
            Map<UUID, AccountSnapshot> accountsById = indexById(accounts, AccountSnapshot::id);
            Map<UUID, CategorySnapshot> categoriesById = indexById(categories, CategorySnapshot::id);

            DashboardHorizon horizon = new DashboardHorizon(today, today.plusDays(HORIZON_DAYS - 1L));
            AvailableToSpend availableToSpend = new AvailableToSpend(summary.balance(), summary.committedAmount(),
                summary.availableToSpend(), summary.shortfall());
            List<AttentionItem> attention = summary.attention().stream()
                .map(item -> toAttentionItem(item, obligationsById, accountsById, categoriesById))
                .toList();
            List<UpcomingCommitment> upcomingCommitments = upcomingCommitments(summary.obligations(),
                obligationsById, accountsById);

            return new Dashboard(horizon, availableToSpend, attention, upcomingCommitments, recent);
        });
    }

    private CommitmentSummary calculate(LocalDate today, List<ObligationSnapshot> obligations,
                                        List<ResolutionSnapshot> resolutions, List<AccountSnapshot> accounts,
                                        List<CategorySnapshot> categories) {
        try {
            return commitmentCalculator.calculate(today, obligations, resolutions, accounts, categories);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Dashboard data is inconsistent: " + e.getMessage(), e);
        }
    }

    private static AttentionItem toAttentionItem(Attention attention, Map<UUID, ObligationSnapshot> obligationsById,
                                                 Map<UUID, AccountSnapshot> accountsById,
                                                 Map<UUID, CategorySnapshot> categoriesById) {
        return switch (attention) {
            case Attention.OverdueOccurrence overdue -> new AttentionItem.OverdueOccurrence(
                obligationSummary(obligationsById.get(overdue.obligationId())), overdue.oldestOverdue(),
                overdue.overdueCount(), overdue.overdueAmount());
            case Attention.Shortfall shortfall -> new AttentionItem.Shortfall(shortfall.shortfall());
            case Attention.PaymentBlocked blocked -> {
                ObligationSnapshot obligation = obligationsById.get(blocked.obligationId());
                AccountSnapshot account = accountsById.get(obligation.accountId());
                CategorySnapshot category = categoriesById.get(obligation.categoryId());
                yield new AttentionItem.PaymentBlocked(obligationSummary(obligation), accountSummary(account),
                    blocked.nearestDueDate(), blocked.committed(), account.status() != AccountStatus.ACTIVE,
                    category.status() != CategoryStatus.ACTIVE);
            }
            case Attention.AccountShortfall accountShortfall -> new AttentionItem.AccountShortfall(
                accountSummary(accountsById.get(accountShortfall.accountId())), accountShortfall.shortfall(),
                accountShortfall.nearestDueDate());
        };
    }

    private static List<UpcomingCommitment> upcomingCommitments(List<ObligationCommitment> commitments,
                                                                Map<UUID, ObligationSnapshot> obligationsById,
                                                                Map<UUID, AccountSnapshot> accountsById) {
        return commitments.stream()
            .flatMap(commitment -> {
                ObligationSnapshot obligation = obligationsById.get(commitment.obligationId());
                ObligationSummary obligationSummary = obligationSummary(obligation);
                AccountSummary account = accountSummary(accountsById.get(obligation.accountId()));
                return commitment.pendingDates().stream()
                    .map(dueDate -> new UpcomingCommitment(obligationSummary, dueDate, obligation.amount(), account,
                        commitment.paymentSourceInactive()));
            })
            .sorted(Comparator.comparing(UpcomingCommitment::dueDate)
                .thenComparing(upcoming -> upcoming.obligation().name())
                .thenComparing(upcoming -> upcoming.obligation().id()))
            .limit(UPCOMING_COMMITMENTS_LIMIT)
            .toList();
    }

    private static ObligationSummary obligationSummary(ObligationSnapshot obligation) {
        return new ObligationSummary(obligation.id(), obligation.name());
    }

    private static AccountSummary accountSummary(AccountSnapshot account) {
        return new AccountSummary(account.id(), account.name());
    }

    private static <T> Map<UUID, T> indexById(List<T> items, Function<T, UUID> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
