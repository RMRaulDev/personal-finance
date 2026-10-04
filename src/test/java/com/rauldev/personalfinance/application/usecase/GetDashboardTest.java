package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.port.out.DashboardQueryPort;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.AttentionItem;
import com.rauldev.personalfinance.application.readmodel.AvailableToSpend;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.Dashboard;
import com.rauldev.personalfinance.application.readmodel.DashboardHorizon;
import com.rauldev.personalfinance.application.readmodel.ObligationSummary;
import com.rauldev.personalfinance.application.readmodel.RecentActivityItem;
import com.rauldev.personalfinance.application.readmodel.UpcomingCommitment;
import com.rauldev.personalfinance.domain.AccountSnapshot;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.CategorySnapshot;
import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.ObligationSnapshot;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.domain.ResolutionSnapshot;

class GetDashboardTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final UUID userId = UUID.randomUUID();
    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();

    private final AccountSnapshot checking = account("Checking", 1_000_000, AccountStatus.ACTIVE);
    private final CategorySnapshot rentCategory = new CategorySnapshot(UUID.randomUUID(), CategoryStatus.ACTIVE);

    private static AccountSnapshot account(String name, long cents, AccountStatus status) {
        return new AccountSnapshot(UUID.randomUUID(), name, Money.ofCents(cents), status);
    }

    private ObligationSnapshot obligation(String name, long cents, AccountSnapshot account, CategorySnapshot category,
                                          Frequency frequency, LocalDate start) {
        return new ObligationSnapshot(UUID.randomUUID(), name, Money.ofCents(cents), account.id(), category.id(),
            new Recurrence(frequency, start, null), ObligationStatus.ACTIVE);
    }

    private ObligationSnapshot once(String name, long cents, LocalDate date) {
        return obligation(name, cents, checking, rentCategory, Frequency.ONCE, date);
    }

    private static LocalDate day(int offsetFromToday) {
        return TODAY.plusDays(offsetFromToday);
    }

    private static ObligationSummary summary(ObligationSnapshot obligation) {
        return new ObligationSummary(obligation.id(), obligation.name());
    }

    private static AccountSummary summary(AccountSnapshot account) {
        return new AccountSummary(account.id(), account.name());
    }

    private RecordingDashboardQueryPort port(List<ObligationSnapshot> obligations,
                                             List<ResolutionSnapshot> resolutions) {
        return port(obligations, resolutions, List.of(checking), List.of(rentCategory), List.of());
    }

    private RecordingDashboardQueryPort port(List<ObligationSnapshot> obligations,
                                             List<ResolutionSnapshot> resolutions,
                                             List<AccountSnapshot> accounts, List<CategorySnapshot> categories,
                                             List<RecentActivityItem> recent) {
        return new RecordingDashboardQueryPort(transactionManager, obligations, resolutions, accounts, categories,
            recent);
    }

    private Dashboard dashboard(RecordingDashboardQueryPort port) {
        return new GetDashboard(port, transactionManager, CLOCK).execute(new GetDashboardQuery(userId));
    }

    private Dashboard dashboardOf(ObligationSnapshot... obligations) {
        return dashboard(port(List.of(obligations), List.of()));
    }

    @Test
    void execute_shouldReturnZeroAmountsAndEmptyListsWhenUserHasNoData() {
        RecordingDashboardQueryPort port = port(List.of(), List.of(), List.of(), List.of(), List.of());

        Dashboard result = dashboard(port);

        assertEquals(new DashboardHorizon(TODAY, TODAY.plusDays(13)), result.horizon());
        assertEquals(new AvailableToSpend(Money.ofCents(0), Money.ofCents(0), Money.ofCents(0), Money.ofCents(0)),
            result.availableToSpend());
        assertEquals(List.of(), result.attention());
        assertEquals(List.of(), result.upcomingCommitments());
        assertEquals(List.of(), result.recent());
    }

    @Test
    void execute_shouldComputeTodayInTheZoneOfTheClock() {
        Clock mexicoCity = Clock.fixed(Instant.parse("2026-10-05T03:00:00Z"), ZoneId.of("America/Mexico_City"));
        RecordingDashboardQueryPort port = port(List.of(), List.of(), List.of(), List.of(), List.of());

        Dashboard result = new GetDashboard(port, transactionManager, mexicoCity)
            .execute(new GetDashboardQuery(userId));

        assertEquals(LocalDate.of(2026, 10, 4), result.horizon().from());
        assertEquals(LocalDate.of(2026, 10, 17), result.horizon().to());
    }

    @Test
    void execute_shouldReadEverythingInsideOneTransactionWithTheUserIdAndRecentLimitOfFive() {
        RecordingDashboardQueryPort port = port(List.of(), List.of(), List.of(), List.of(), List.of());

        dashboard(port);

        assertEquals(1, transactionManager.executions);
        assertEquals(List.of("obligations", "resolutions", "accounts", "categories", "recent"), port.calls);
        assertEquals(List.of(true, true, true, true, true), port.insideTransaction);
        assertEquals(List.of(userId, userId, userId, userId, userId), port.userIds);
        assertEquals(5, port.recentLimit);
        assertFalse(transactionManager.active);
    }

    @Test
    void execute_shouldReportAvailableAndNoShortfallWhenBalanceCoversCommitments() {
        AccountSnapshot account = account("Checking", 1000, AccountStatus.ACTIVE);
        ObligationSnapshot rent = obligation("Rent", 400, account, rentCategory, Frequency.ONCE, TODAY);

        Dashboard result = dashboard(port(List.of(rent), List.of(), List.of(account), List.of(rentCategory),
            List.of()));

        assertEquals(new AvailableToSpend(Money.ofCents(1000), Money.ofCents(400), Money.ofCents(600),
            Money.ofCents(0)), result.availableToSpend());
        assertEquals(List.of(), result.attention());
    }

    @Test
    void execute_shouldReportShortfallAndNoAvailableWhenCommitmentsExceedBalance() {
        AccountSnapshot account = account("Checking", 300, AccountStatus.ACTIVE);
        ObligationSnapshot rent = obligation("Rent", 1000, account, rentCategory, Frequency.ONCE, TODAY);

        Dashboard result = dashboard(port(List.of(rent), List.of(), List.of(account), List.of(rentCategory),
            List.of()));

        assertEquals(new AvailableToSpend(Money.ofCents(300), Money.ofCents(1000), Money.ofCents(0),
            Money.ofCents(700)), result.availableToSpend());
    }

    @Test
    void execute_shouldMapOverdueOccurrenceAttentionWithObligationName() {
        ObligationSnapshot weekly = obligation("Rent", 100, checking, rentCategory, Frequency.WEEKLY, day(-14));

        Dashboard result = dashboardOf(weekly);

        assertEquals(List.of(new AttentionItem.OverdueOccurrence(summary(weekly), day(-14), 2, Money.ofCents(200))),
            result.attention());
    }

    @Test
    void execute_shouldIgnoreResolvedOccurrencesInOverdueAttention() {
        ObligationSnapshot weekly = obligation("Rent", 100, checking, rentCategory, Frequency.WEEKLY, day(-14));

        Dashboard result = dashboard(port(List.of(weekly),
            List.of(new ResolutionSnapshot(weekly.id(), day(-14)))));

        assertEquals(List.of(new AttentionItem.OverdueOccurrence(summary(weekly), day(-7), 1, Money.ofCents(100))),
            result.attention());
    }

    @Test
    void execute_shouldMapShortfallAttention() {
        AccountSnapshot account = account("Checking", 300, AccountStatus.ACTIVE);
        ObligationSnapshot rent = obligation("Rent", 1000, account, rentCategory, Frequency.ONCE, day(2));

        Dashboard result = dashboard(port(List.of(rent), List.of(), List.of(account), List.of(rentCategory),
            List.of()));

        assertTrue(result.attention().contains(new AttentionItem.Shortfall(Money.ofCents(700))));
    }

    @Test
    void execute_shouldMapAccountShortfallAttentionWithAccountName() {
        AccountSnapshot account = account("Savings", 300, AccountStatus.ACTIVE);
        ObligationSnapshot rent = obligation("Rent", 1000, account, rentCategory, Frequency.ONCE, day(2));

        Dashboard result = dashboard(port(List.of(rent), List.of(), List.of(account), List.of(rentCategory),
            List.of()));

        assertTrue(result.attention().contains(
            new AttentionItem.AccountShortfall(summary(account), Money.ofCents(700), day(2))));
    }

    @Test
    void execute_shouldMapPaymentBlockedByInactiveAccountOnly() {
        AccountSnapshot inactive = account("Old card", 0, AccountStatus.INACTIVE);
        ObligationSnapshot gym = obligation("Gym", 200, inactive, rentCategory, Frequency.ONCE, day(2));

        Dashboard result = dashboard(port(List.of(gym), List.of(), List.of(checking, inactive),
            List.of(rentCategory), List.of()));

        assertTrue(result.attention().contains(new AttentionItem.PaymentBlocked(summary(gym), summary(inactive),
            day(2), Money.ofCents(200), true, false)));
    }

    @Test
    void execute_shouldMapPaymentBlockedByInactiveCategoryOnly() {
        CategorySnapshot inactiveCategory = new CategorySnapshot(UUID.randomUUID(), CategoryStatus.INACTIVE);
        ObligationSnapshot netflix = obligation("Netflix", 50, checking, inactiveCategory, Frequency.ONCE, day(1));

        Dashboard result = dashboard(port(List.of(netflix), List.of(), List.of(checking),
            List.of(rentCategory, inactiveCategory), List.of()));

        assertEquals(List.of(new AttentionItem.PaymentBlocked(summary(netflix), summary(checking), day(1),
            Money.ofCents(50), false, true)), result.attention());
    }

    @Test
    void execute_shouldMapPaymentBlockedWhenBothAccountAndCategoryAreInactive() {
        AccountSnapshot inactive = account("Old card", 0, AccountStatus.INACTIVE);
        CategorySnapshot inactiveCategory = new CategorySnapshot(UUID.randomUUID(), CategoryStatus.INACTIVE);
        ObligationSnapshot gym = obligation("Gym", 200, inactive, inactiveCategory, Frequency.ONCE, day(2));

        Dashboard result = dashboard(port(List.of(gym), List.of(), List.of(inactive),
            List.of(inactiveCategory), List.of()));

        assertEquals(List.of(new AttentionItem.Shortfall(Money.ofCents(200)),
            new AttentionItem.PaymentBlocked(summary(gym), summary(inactive), day(2), Money.ofCents(200), true,
                true)), result.attention());
    }

    @Test
    void execute_shouldPreserveTheCalculatorOrderOfAttention() {
        AccountSnapshot account = account("Checking", 100, AccountStatus.ACTIVE);
        AccountSnapshot inactive = account("Old card", 0, AccountStatus.INACTIVE);
        ObligationSnapshot overdue = obligation("Rent", 500, account, rentCategory, Frequency.ONCE, day(-3));
        ObligationSnapshot blockedLater = obligation("Gym", 200, inactive, rentCategory, Frequency.ONCE, day(2));
        ObligationSnapshot blockedSooner = obligation("Netflix", 50, inactive, rentCategory, Frequency.ONCE,
            day(1));

        Dashboard result = dashboard(port(List.of(blockedLater, overdue, blockedSooner), List.of(),
            List.of(account, inactive), List.of(rentCategory), List.of()));

        assertEquals(List.of(
            new AttentionItem.OverdueOccurrence(summary(overdue), day(-3), 1, Money.ofCents(500)),
            new AttentionItem.Shortfall(Money.ofCents(650)),
            new AttentionItem.PaymentBlocked(summary(blockedSooner), summary(inactive), day(1), Money.ofCents(50),
                true, false),
            new AttentionItem.PaymentBlocked(summary(blockedLater), summary(inactive), day(2), Money.ofCents(200),
                true, false),
            new AttentionItem.AccountShortfall(summary(account), Money.ofCents(400), day(-3))),
            result.attention());
    }

    @Test
    void execute_shouldListOneUpcomingCommitmentPerPendingDateWithTheObligationAmount() {
        ObligationSnapshot weekly = obligation("Gym", 250, checking, rentCategory, Frequency.WEEKLY, TODAY);

        Dashboard result = dashboardOf(weekly);

        assertEquals(List.of(
            new UpcomingCommitment(summary(weekly), TODAY, Money.ofCents(250), summary(checking), false),
            new UpcomingCommitment(summary(weekly), day(7), Money.ofCents(250), summary(checking), false)),
            result.upcomingCommitments());
    }

    @Test
    void execute_shouldIncludeTodayAndHorizonEndAndExcludeTheDayAfter() {
        ObligationSnapshot first = once("First", 100, TODAY);
        ObligationSnapshot last = once("Last", 100, day(13));
        ObligationSnapshot beyond = once("Beyond", 100, day(14));

        Dashboard result = dashboardOf(first, last, beyond);

        assertEquals(List.of(TODAY, day(13)),
            result.upcomingCommitments().stream().map(UpcomingCommitment::dueDate).toList());
    }

    @Test
    void execute_shouldExcludeOverdueDatesFromUpcomingCommitments() {
        ObligationSnapshot weekly = obligation("Rent", 100, checking, rentCategory, Frequency.WEEKLY, day(-7));

        Dashboard result = dashboardOf(weekly);

        assertEquals(List.of(TODAY, day(7)),
            result.upcomingCommitments().stream().map(UpcomingCommitment::dueDate).toList());
    }

    @Test
    void execute_shouldExcludeDatesResolvedInAdvanceFromUpcomingCommitments() {
        ObligationSnapshot weekly = obligation("Rent", 100, checking, rentCategory, Frequency.WEEKLY, TODAY);

        Dashboard result = dashboard(port(List.of(weekly), List.of(new ResolutionSnapshot(weekly.id(), day(7)))));

        assertEquals(List.of(TODAY),
            result.upcomingCommitments().stream().map(UpcomingCommitment::dueDate).toList());
    }

    @Test
    void execute_shouldOrderUpcomingCommitmentsByDateThenNameThenId() {
        ObligationSnapshot laterDate = once("Aaa", 100, day(3));
        ObligationSnapshot sameDateB = once("Bbb", 100, day(1));
        ObligationSnapshot sameDateAHigherId = new ObligationSnapshot(new UUID(0, 2), "Aaa", Money.ofCents(100),
            checking.id(), rentCategory.id(), new Recurrence(Frequency.ONCE, day(1), null), ObligationStatus.ACTIVE);
        ObligationSnapshot sameDateALowerId = new ObligationSnapshot(new UUID(0, 1), "Aaa", Money.ofCents(100),
            checking.id(), rentCategory.id(), new Recurrence(Frequency.ONCE, day(1), null), ObligationStatus.ACTIVE);

        Dashboard result = dashboardOf(laterDate, sameDateB, sameDateAHigherId, sameDateALowerId);

        assertEquals(List.of(sameDateALowerId.id(), sameDateAHigherId.id(), sameDateB.id(), laterDate.id()),
            result.upcomingCommitments().stream().map(upcoming -> upcoming.obligation().id()).toList());
    }

    @Test
    void execute_shouldKeepOnlyTheFirstFiveUpcomingCommitmentsWhenThereAreSix() {
        List<ObligationSnapshot> obligations = new ArrayList<>();
        for (int i = 6; i >= 1; i--) {
            obligations.add(once("Obligation " + i, 100, day(i)));
        }

        Dashboard result = dashboard(port(obligations, List.of()));

        assertEquals(List.of(day(1), day(2), day(3), day(4), day(5)),
            result.upcomingCommitments().stream().map(UpcomingCommitment::dueDate).toList());
    }

    @Test
    void execute_shouldKeepFiveUpcomingCommitmentsWhenThereAreExactlyFive() {
        List<ObligationSnapshot> obligations = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            obligations.add(once("Obligation " + i, 100, day(i)));
        }

        Dashboard result = dashboard(port(obligations, List.of()));

        assertEquals(5, result.upcomingCommitments().size());
    }

    @Test
    void execute_shouldMarkUpcomingCommitmentsAsBlockedWhenTheAccountIsInactive() {
        AccountSnapshot inactive = account("Old card", 0, AccountStatus.INACTIVE);
        ObligationSnapshot gym = obligation("Gym", 200, inactive, rentCategory, Frequency.ONCE, day(2));
        ObligationSnapshot rent = once("Rent", 100, day(3));

        Dashboard result = dashboard(port(List.of(gym, rent), List.of(), List.of(checking, inactive),
            List.of(rentCategory), List.of()));

        assertEquals(List.of(
            new UpcomingCommitment(summary(gym), day(2), Money.ofCents(200), summary(inactive), true),
            new UpcomingCommitment(summary(rent), day(3), Money.ofCents(100), summary(checking), false)),
            result.upcomingCommitments());
    }

    @Test
    void execute_shouldMarkUpcomingCommitmentsAsBlockedWhenTheCategoryIsInactive() {
        CategorySnapshot inactiveCategory = new CategorySnapshot(UUID.randomUUID(), CategoryStatus.INACTIVE);
        ObligationSnapshot netflix = obligation("Netflix", 50, checking, inactiveCategory, Frequency.ONCE, day(1));

        Dashboard result = dashboard(port(List.of(netflix), List.of(), List.of(checking),
            List.of(inactiveCategory), List.of()));

        assertTrue(result.upcomingCommitments().get(0).paymentBlocked());
    }

    @Test
    void execute_shouldPassRecentActivityThroughUnchanged() {
        RecentActivityItem income = new RecentActivityItem(UUID.randomUUID(), OperationType.INCOME,
            Money.ofCents(100), TODAY, summary(checking), new CategorySummary(UUID.randomUUID(), "Salary"), null,
            null);
        RecentActivityItem expense = new RecentActivityItem(UUID.randomUUID(), OperationType.EXPENSE,
            Money.ofCents(50), day(-1), summary(checking), new CategorySummary(UUID.randomUUID(), "Rent"), null,
            new ObligationSummary(UUID.randomUUID(), "Rent"));

        Dashboard result = dashboard(port(List.of(), List.of(), List.of(checking), List.of(rentCategory),
            List.of(income, expense)));

        assertEquals(List.of(income, expense), result.recent());
    }

    @Test
    void execute_shouldThrowIllegalStateExceptionWhenTheAccountOfAnActiveObligationIsMissing() {
        ObligationSnapshot orphan = obligation("Rent", 100, account("Ghost", 0, AccountStatus.ACTIVE), rentCategory,
            Frequency.ONCE, TODAY);
        RecordingDashboardQueryPort port = port(List.of(orphan), List.of());

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> dashboard(port));

        assertTrue(e.getMessage().startsWith("Dashboard data is inconsistent: "));
        assertInstanceOf(IllegalArgumentException.class, e.getCause());
        assertEquals("Dashboard data is inconsistent: " + e.getCause().getMessage(), e.getMessage());
    }

    @Test
    void execute_shouldThrowIllegalStateExceptionWhenTheCategoryOfAnActiveObligationIsMissing() {
        CategorySnapshot ghost = new CategorySnapshot(UUID.randomUUID(), CategoryStatus.ACTIVE);
        ObligationSnapshot orphan = obligation("Rent", 100, checking, ghost, Frequency.ONCE, TODAY);
        RecordingDashboardQueryPort port = port(List.of(orphan), List.of());

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> dashboard(port));

        assertTrue(e.getMessage().startsWith("Dashboard data is inconsistent: "));
        assertInstanceOf(IllegalArgumentException.class, e.getCause());
        assertEquals("Dashboard data is inconsistent: " + e.getCause().getMessage(), e.getMessage());
    }

    @Test
    void execute_shouldPropagateAnIllegalArgumentExceptionFromThePortUnwrappedAndStopReading() {
        IllegalArgumentException failure = new IllegalArgumentException("port failure");
        RecordingDashboardQueryPort port = port(List.of(), List.of(), List.of(), List.of(), List.of());
        port.recentFailure = failure;

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> dashboard(port));

        assertSame(failure, e);
        assertEquals(List.of("obligations", "resolutions", "accounts", "categories", "recent"), port.calls);
        assertFalse(transactionManager.active);
    }

    @Test
    void execute_shouldRejectNullQuery() {
        RecordingDashboardQueryPort port = port(List.of(), List.of());
        GetDashboard useCase = new GetDashboard(port, transactionManager, CLOCK);

        NullPointerException e = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("Query cannot be null", e.getMessage());
        assertEquals(0, transactionManager.executions);
        assertEquals(List.of(), port.calls);
    }

    @Test
    void query_shouldRejectNullUserId() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new GetDashboardQuery(null));

        assertEquals("User id cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldRejectNullDashboardQueryPort() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new GetDashboard(null, transactionManager, CLOCK));

        assertEquals("Dashboard query port cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldRejectNullTransactionManager() {
        RecordingDashboardQueryPort port = port(List.of(), List.of());

        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new GetDashboard(port, null, CLOCK));

        assertEquals("Transaction manager cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldRejectNullClock() {
        RecordingDashboardQueryPort port = port(List.of(), List.of());

        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new GetDashboard(port, transactionManager, null));

        assertEquals("Clock cannot be null", e.getMessage());
    }

    private static final class RecordingTransactionManager implements TransactionManager {
        private int executions;
        private boolean active;

        @Override
        public <T> T execute(Supplier<T> transactionalWork) {
            executions++;
            active = true;
            try {
                return transactionalWork.get();
            } finally {
                active = false;
            }
        }
    }

    private static final class RecordingDashboardQueryPort implements DashboardQueryPort {
        private final RecordingTransactionManager transactionManager;
        private final List<ObligationSnapshot> obligations;
        private final List<ResolutionSnapshot> resolutions;
        private final List<AccountSnapshot> accounts;
        private final List<CategorySnapshot> categories;
        private final List<RecentActivityItem> recent;
        private final List<String> calls = new ArrayList<>();
        private final List<Boolean> insideTransaction = new ArrayList<>();
        private final List<UUID> userIds = new ArrayList<>();
        private int recentLimit;
        private RuntimeException recentFailure;

        private RecordingDashboardQueryPort(RecordingTransactionManager transactionManager,
                                            List<ObligationSnapshot> obligations,
                                            List<ResolutionSnapshot> resolutions, List<AccountSnapshot> accounts,
                                            List<CategorySnapshot> categories, List<RecentActivityItem> recent) {
            this.transactionManager = transactionManager;
            this.obligations = obligations;
            this.resolutions = resolutions;
            this.accounts = accounts;
            this.categories = categories;
            this.recent = recent;
        }

        private void record(String call, UUID userId) {
            calls.add(call);
            insideTransaction.add(transactionManager.active);
            userIds.add(userId);
        }

        @Override
        public List<ObligationSnapshot> findActiveObligations(UUID userId) {
            record("obligations", userId);
            return obligations;
        }

        @Override
        public List<ResolutionSnapshot> findResolutionsOfActiveObligations(UUID userId) {
            record("resolutions", userId);
            return resolutions;
        }

        @Override
        public List<AccountSnapshot> findAccounts(UUID userId) {
            record("accounts", userId);
            return accounts;
        }

        @Override
        public List<CategorySnapshot> findCategories(UUID userId) {
            record("categories", userId);
            return categories;
        }

        @Override
        public List<RecentActivityItem> findRecentActivity(UUID userId, int limit) {
            record("recent", userId);
            recentLimit = limit;
            if (recentFailure != null) {
                throw recentFailure;
            }
            return recent;
        }
    }
}
