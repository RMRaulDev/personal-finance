package com.rauldev.personalfinance.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.AttentionItem;
import com.rauldev.personalfinance.application.readmodel.AvailableToSpend;
import com.rauldev.personalfinance.application.readmodel.CategoryDetails;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.Dashboard;
import com.rauldev.personalfinance.application.readmodel.DashboardHorizon;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationDetails;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationHistoryItem;
import com.rauldev.personalfinance.application.readmodel.ObligationSummary;
import com.rauldev.personalfinance.application.readmodel.RecentActivityItem;
import com.rauldev.personalfinance.application.readmodel.TransferDetails;
import com.rauldev.personalfinance.application.readmodel.UpcomingCommitment;
import com.rauldev.personalfinance.domain.AttentionType;
import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;

class ReadModelValidationTest {
    private static final LocalDate OPERATION_DATE = LocalDate.of(2026, 8, 24);

    @Test
    void accountSummaryRequiresIdAndName() {
        assertDoesNotThrow(() -> new AccountSummary(UUID.randomUUID(), "Checking"));

        assertThrows(NullPointerException.class, () -> new AccountSummary(null, "Checking"));
        assertThrows(NullPointerException.class, () -> new AccountSummary(UUID.randomUUID(), null));
    }

    @Test
    void categorySummaryRequiresIdAndName() {
        assertDoesNotThrow(() -> new CategorySummary(UUID.randomUUID(), "Food"));

        assertThrows(NullPointerException.class, () -> new CategorySummary(null, "Food"));
        assertThrows(NullPointerException.class, () -> new CategorySummary(UUID.randomUUID(), null));
    }

    @Test
    void categoryDetailsAcceptsValidValues() {
        UUID id = UUID.randomUUID();

        CategoryDetails details = new CategoryDetails(id, "Food", CategoryType.EXPENSE, CategoryStatus.INACTIVE);

        assertEquals(id, details.id());
        assertEquals("Food", details.name());
        assertEquals(CategoryType.EXPENSE, details.type());
        assertEquals(CategoryStatus.INACTIVE, details.status());
    }

    @Test
    void categoryDetailsRejectsNullFieldsWithMessages() {
        UUID id = UUID.randomUUID();

        assertEquals("Category id cannot be null", assertThrows(NullPointerException.class,
            () -> new CategoryDetails(null, "Food", CategoryType.EXPENSE, CategoryStatus.ACTIVE)).getMessage());
        assertEquals("Category name cannot be null", assertThrows(NullPointerException.class,
            () -> new CategoryDetails(id, null, CategoryType.EXPENSE, CategoryStatus.ACTIVE)).getMessage());
        assertEquals("Category type cannot be null", assertThrows(NullPointerException.class,
            () -> new CategoryDetails(id, "Food", null, CategoryStatus.ACTIVE)).getMessage());
        assertEquals("Category status cannot be null", assertThrows(NullPointerException.class,
            () -> new CategoryDetails(id, "Food", CategoryType.EXPENSE, null)).getMessage());
    }

    @Test
    void categoryDetailsRejectsBlankName() {
        UUID id = UUID.randomUUID();

        assertEquals("Category name cannot be empty", assertThrows(IllegalArgumentException.class,
            () -> new CategoryDetails(id, "", CategoryType.INCOME, CategoryStatus.ACTIVE)).getMessage());
        assertEquals("Category name cannot be empty", assertThrows(IllegalArgumentException.class,
            () -> new CategoryDetails(id, "   ", CategoryType.INCOME, CategoryStatus.ACTIVE)).getMessage());
    }

    @Test
    void transferDetailsRequiresBothAccounts() {
        AccountSummary source = new AccountSummary(UUID.randomUUID(), "Source");
        AccountSummary target = new AccountSummary(UUID.randomUUID(), "Target");

        assertDoesNotThrow(() -> new TransferDetails(source, target));
        assertThrows(NullPointerException.class, () -> new TransferDetails(null, target));
        assertThrows(NullPointerException.class, () -> new TransferDetails(source, null));
    }

    @Test
    void financialOperationHistoryItemAcceptsValidIncomeExpenseAndTransfer() {
        AccountSummary account = new AccountSummary(UUID.randomUUID(), "Checking");
        CategorySummary category = new CategorySummary(UUID.randomUUID(), "Food");
        TransferDetails transfer = new TransferDetails(
            new AccountSummary(UUID.randomUUID(), "Source"),
            new AccountSummary(UUID.randomUUID(), "Target"));

        assertDoesNotThrow(() -> new FinancialOperationHistoryItem(
            UUID.randomUUID(),
            OperationType.INCOME,
            Money.of(BigDecimal.valueOf(150.00)),
            OPERATION_DATE,
            OperationStatus.ACTIVE,
            null,
            account,
            category,
            null));

        assertDoesNotThrow(() -> new FinancialOperationHistoryItem(
            UUID.randomUUID(),
            OperationType.EXPENSE,
            Money.of(BigDecimal.valueOf(75.50)),
            OPERATION_DATE,
            OperationStatus.ACTIVE,
            null,
            account,
            category,
            null));

        assertDoesNotThrow(() -> new FinancialOperationHistoryItem(
            UUID.randomUUID(),
            OperationType.TRANSFER,
            Money.of(BigDecimal.valueOf(200.00)),
            OPERATION_DATE,
            null,
            null,
            null,
            null,
            transfer));
    }

    @Test
    void financialOperationHistoryItemRejectsInvalidRequiredFieldsAndTypeRules() {
        AccountSummary account = new AccountSummary(UUID.randomUUID(), "Checking");
        CategorySummary category = new CategorySummary(UUID.randomUUID(), "Food");
        TransferDetails transfer = new TransferDetails(
            new AccountSummary(UUID.randomUUID(), "Source"),
            new AccountSummary(UUID.randomUUID(), "Target"));

        assertThrows(NullPointerException.class, () -> new FinancialOperationHistoryItem(
            null, OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, null));
        assertThrows(NullPointerException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), null, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, null));
        assertThrows(NullPointerException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, null, OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, null));
        assertThrows(NullPointerException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), null, OperationStatus.ACTIVE, null, account, category, null));

        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, null, category, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.EXPENSE, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, null, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.EXPENSE, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.TRANSFER, Money.of(BigDecimal.TEN), OPERATION_DATE, null, null, account, null, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.TRANSFER, Money.of(BigDecimal.TEN), OPERATION_DATE, null, null, null, category, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.TRANSFER, Money.of(BigDecimal.TEN), OPERATION_DATE, null, null, null, null, null));
        assertThrows(NullPointerException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, null, null, account, category, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.TRANSFER, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, null, null, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.CANCELLED, null, account, category, null));
        assertDoesNotThrow(() -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.CANCELLED, Instant.now(), account, category, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, Instant.now(), account, category, null));
    }

    @Test
    void financialOperationDetailsHasSameInvariantsAsHistoryItem() {
        AccountSummary account = new AccountSummary(UUID.randomUUID(), "Checking");
        CategorySummary category = new CategorySummary(UUID.randomUUID(), "Food");
        TransferDetails transfer = new TransferDetails(
            new AccountSummary(UUID.randomUUID(), "Source"),
            new AccountSummary(UUID.randomUUID(), "Target"));

        assertDoesNotThrow(() -> new FinancialOperationDetails(
            UUID.randomUUID(),
            OperationType.INCOME,
            Money.of(BigDecimal.valueOf(100.00)),
            OPERATION_DATE,
            OperationStatus.ACTIVE,
            null,
            account,
            category,
            null));

        assertDoesNotThrow(() -> new FinancialOperationDetails(
            UUID.randomUUID(),
            OperationType.TRANSFER,
            Money.of(BigDecimal.valueOf(250.00)),
            OPERATION_DATE,
            null,
            null,
            null,
            null,
            transfer));

        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationDetails(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationDetails(
            UUID.randomUUID(), OperationType.TRANSFER, Money.of(BigDecimal.TEN), OPERATION_DATE, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationDetails(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, Instant.now(), account, category, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationDetails(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.CANCELLED, null, account, category, null));
        assertDoesNotThrow(() -> new FinancialOperationDetails(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.CANCELLED, Instant.now(), account, category, null));
    }

    // ---- Dashboard read models ----

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);

    private static Money cents(long value) {
        return Money.ofCents(value);
    }

    private static ObligationSummary obligationSummary() {
        return new ObligationSummary(UUID.randomUUID(), "Rent");
    }

    private static AccountSummary accountSummary() {
        return new AccountSummary(UUID.randomUUID(), "Checking");
    }

    private static AvailableToSpend availableToSpend() {
        return new AvailableToSpend(cents(1000), cents(400), cents(600), cents(0));
    }

    @Test
    void obligationSummaryRequiresIdAndName() {
        assertDoesNotThrow(() -> new ObligationSummary(UUID.randomUUID(), "Rent"));

        NullPointerException noId = assertThrows(NullPointerException.class, () -> new ObligationSummary(null, "Rent"));
        NullPointerException noName = assertThrows(NullPointerException.class,
            () -> new ObligationSummary(UUID.randomUUID(), null));
        assertEquals("Obligation summary id cannot be null", noId.getMessage());
        assertEquals("Obligation summary name cannot be null", noName.getMessage());
    }

    @Test
    void dashboardHorizonAcceptsStartBeforeAndEqualToEnd() {
        assertDoesNotThrow(() -> new DashboardHorizon(TODAY, TODAY.plusDays(13)));
        assertDoesNotThrow(() -> new DashboardHorizon(TODAY, TODAY));
    }

    @Test
    void dashboardHorizonRejectsStartAfterEnd() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new DashboardHorizon(TODAY.plusDays(1), TODAY));

        assertEquals("Horizon start cannot be after its end", e.getMessage());
    }

    @Test
    void dashboardHorizonRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new DashboardHorizon(null, TODAY));
        assertThrows(NullPointerException.class, () -> new DashboardHorizon(TODAY, null));
    }

    @Test
    void availableToSpendAcceptsPositiveAvailable() {
        AvailableToSpend result = new AvailableToSpend(cents(1000), cents(400), cents(600), cents(0));

        assertEquals(cents(600), result.available());
        assertEquals(cents(0), result.shortfall());
    }

    @Test
    void availableToSpendAcceptsPositiveShortfall() {
        AvailableToSpend result = new AvailableToSpend(cents(300), cents(1000), cents(0), cents(700));

        assertEquals(cents(0), result.available());
        assertEquals(cents(700), result.shortfall());
    }

    @Test
    void availableToSpendAcceptsBothZeroWhenBalanceEqualsCommitted() {
        assertDoesNotThrow(() -> new AvailableToSpend(cents(500), cents(500), cents(0), cents(0)));
        assertDoesNotThrow(() -> new AvailableToSpend(cents(0), cents(0), cents(0), cents(0)));
    }

    @Test
    void availableToSpendRejectsBothPositive() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new AvailableToSpend(cents(1000), cents(1000), cents(100), cents(100)));

        assertEquals("Available and shortfall cannot both be positive", e.getMessage());
    }

    @Test
    void availableToSpendRejectsInconsistentSum() {
        IllegalArgumentException available = assertThrows(IllegalArgumentException.class,
            () -> new AvailableToSpend(cents(1000), cents(400), cents(601), cents(0)));
        IllegalArgumentException shortfall = assertThrows(IllegalArgumentException.class,
            () -> new AvailableToSpend(cents(300), cents(1000), cents(0), cents(699)));
        IllegalArgumentException bothZero = assertThrows(IllegalArgumentException.class,
            () -> new AvailableToSpend(cents(500), cents(400), cents(0), cents(0)));

        assertEquals("Available and shortfall are inconsistent with balance and committed", available.getMessage());
        assertEquals("Available and shortfall are inconsistent with balance and committed", shortfall.getMessage());
        assertEquals("Available and shortfall are inconsistent with balance and committed", bothZero.getMessage());
    }

    @Test
    void availableToSpendRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new AvailableToSpend(null, cents(0), cents(0), cents(0)));
        assertThrows(NullPointerException.class, () -> new AvailableToSpend(cents(0), null, cents(0), cents(0)));
        assertThrows(NullPointerException.class, () -> new AvailableToSpend(cents(0), cents(0), null, cents(0)));
        assertThrows(NullPointerException.class, () -> new AvailableToSpend(cents(0), cents(0), cents(0), null));
    }

    @Test
    void overdueOccurrenceAttentionAcceptsValidValuesAndReportsItsType() {
        AttentionItem item = new AttentionItem.OverdueOccurrence(obligationSummary(), TODAY, 1, cents(100));

        assertEquals(AttentionType.OVERDUE_OCCURRENCE, item.type());
    }

    @Test
    void overdueOccurrenceAttentionRejectsZeroAndNegativeCount() {
        IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
            () -> new AttentionItem.OverdueOccurrence(obligationSummary(), TODAY, 0, cents(100)));
        assertThrows(IllegalArgumentException.class,
            () -> new AttentionItem.OverdueOccurrence(obligationSummary(), TODAY, -1, cents(100)));

        assertEquals("Overdue count must be greater than zero", zero.getMessage());
    }

    @Test
    void overdueOccurrenceAttentionRejectsNulls() {
        assertThrows(NullPointerException.class,
            () -> new AttentionItem.OverdueOccurrence(null, TODAY, 1, cents(100)));
        assertThrows(NullPointerException.class,
            () -> new AttentionItem.OverdueOccurrence(obligationSummary(), null, 1, cents(100)));
        assertThrows(NullPointerException.class,
            () -> new AttentionItem.OverdueOccurrence(obligationSummary(), TODAY, 1, null));
    }

    @Test
    void shortfallAttentionAcceptsValueReportsItsTypeAndRejectsNull() {
        assertEquals(AttentionType.SHORTFALL, new AttentionItem.Shortfall(cents(100)).type());

        assertThrows(NullPointerException.class, () -> new AttentionItem.Shortfall(null));
    }

    @Test
    void paymentBlockedAttentionAcceptsInactiveAccountInactiveCategoryOrBoth() {
        AttentionItem account = new AttentionItem.PaymentBlocked(obligationSummary(), accountSummary(), TODAY,
            cents(100), true, false);

        assertDoesNotThrow(() -> new AttentionItem.PaymentBlocked(obligationSummary(), accountSummary(), TODAY,
            cents(100), false, true));
        assertDoesNotThrow(() -> new AttentionItem.PaymentBlocked(obligationSummary(), accountSummary(), TODAY,
            cents(100), true, true));
        assertEquals(AttentionType.PAYMENT_BLOCKED, account.type());
    }

    @Test
    void paymentBlockedAttentionRejectsWhenNothingIsInactive() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new AttentionItem.PaymentBlocked(obligationSummary(), accountSummary(), TODAY, cents(100),
                false, false));

        assertEquals("A blocked payment requires an inactive account or category", e.getMessage());
    }

    @Test
    void paymentBlockedAttentionRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new AttentionItem.PaymentBlocked(null, accountSummary(),
            TODAY, cents(100), true, false));
        assertThrows(NullPointerException.class, () -> new AttentionItem.PaymentBlocked(obligationSummary(), null,
            TODAY, cents(100), true, false));
        assertThrows(NullPointerException.class, () -> new AttentionItem.PaymentBlocked(obligationSummary(),
            accountSummary(), null, cents(100), true, false));
        assertThrows(NullPointerException.class, () -> new AttentionItem.PaymentBlocked(obligationSummary(),
            accountSummary(), TODAY, null, true, false));
    }

    @Test
    void accountShortfallAttentionAcceptsValuesReportsItsTypeAndRejectsNulls() {
        AttentionItem item = new AttentionItem.AccountShortfall(accountSummary(), cents(100), TODAY);

        assertEquals(AttentionType.ACCOUNT_SHORTFALL, item.type());
        assertThrows(NullPointerException.class, () -> new AttentionItem.AccountShortfall(null, cents(100), TODAY));
        assertThrows(NullPointerException.class,
            () -> new AttentionItem.AccountShortfall(accountSummary(), null, TODAY));
        assertThrows(NullPointerException.class,
            () -> new AttentionItem.AccountShortfall(accountSummary(), cents(100), null));
    }

    @Test
    void upcomingCommitmentAcceptsValidValuesWithEitherBlockedFlag() {
        assertDoesNotThrow(() -> new UpcomingCommitment(obligationSummary(), TODAY, cents(100), accountSummary(),
            false));
        assertDoesNotThrow(() -> new UpcomingCommitment(obligationSummary(), TODAY, cents(100), accountSummary(),
            true));
    }

    @Test
    void upcomingCommitmentRejectsNulls() {
        assertThrows(NullPointerException.class,
            () -> new UpcomingCommitment(null, TODAY, cents(100), accountSummary(), false));
        assertThrows(NullPointerException.class,
            () -> new UpcomingCommitment(obligationSummary(), null, cents(100), accountSummary(), false));
        assertThrows(NullPointerException.class,
            () -> new UpcomingCommitment(obligationSummary(), TODAY, null, accountSummary(), false));
        assertThrows(NullPointerException.class,
            () -> new UpcomingCommitment(obligationSummary(), TODAY, cents(100), null, false));
    }

    private static RecentActivityItem recent(OperationType type, AccountSummary account, CategorySummary category,
                                             TransferDetails transfer, ObligationSummary obligation) {
        return new RecentActivityItem(UUID.randomUUID(), type, cents(100), TODAY, account, category, transfer,
            obligation);
    }

    private static TransferDetails transferDetails() {
        return new TransferDetails(new AccountSummary(UUID.randomUUID(), "Source"),
            new AccountSummary(UUID.randomUUID(), "Target"));
    }

    private static CategorySummary categorySummary() {
        return new CategorySummary(UUID.randomUUID(), "Food");
    }

    @Test
    void recentActivityItemAcceptsValidIncomeExpenseAndTransferShapes() {
        assertDoesNotThrow(() -> recent(OperationType.INCOME, accountSummary(), categorySummary(), null, null));
        assertDoesNotThrow(() -> recent(OperationType.EXPENSE, accountSummary(), categorySummary(), null, null));
        assertDoesNotThrow(() -> recent(OperationType.TRANSFER, null, null, transferDetails(), null));
    }

    @Test
    void recentActivityItemAcceptsObligationOnExpense() {
        ObligationSummary obligation = obligationSummary();

        RecentActivityItem item = recent(OperationType.EXPENSE, accountSummary(), categorySummary(), null,
            obligation);

        assertEquals(obligation, item.obligation());
    }

    @Test
    void recentActivityItemRejectsObligationOnIncomeAndTransfer() {
        IllegalArgumentException income = assertThrows(IllegalArgumentException.class,
            () -> recent(OperationType.INCOME, accountSummary(), categorySummary(), null, obligationSummary()));
        IllegalArgumentException transfer = assertThrows(IllegalArgumentException.class,
            () -> recent(OperationType.TRANSFER, null, null, transferDetails(), obligationSummary()));

        assertEquals("Only expense operations can reference an obligation", income.getMessage());
        assertEquals("Only expense operations can reference an obligation", transfer.getMessage());
    }

    @Test
    void recentActivityItemRejectsInvalidIncomeAndExpenseCombinations() {
        for (OperationType type : List.of(OperationType.INCOME, OperationType.EXPENSE)) {
            assertThrows(IllegalArgumentException.class,
                () -> recent(type, null, categorySummary(), null, null));
            assertThrows(IllegalArgumentException.class,
                () -> recent(type, accountSummary(), null, null, null));
            assertThrows(IllegalArgumentException.class,
                () -> recent(type, accountSummary(), categorySummary(), transferDetails(), null));
        }
    }

    @Test
    void recentActivityItemRejectsInvalidTransferCombinations() {
        assertThrows(IllegalArgumentException.class,
            () -> recent(OperationType.TRANSFER, accountSummary(), null, transferDetails(), null));
        assertThrows(IllegalArgumentException.class,
            () -> recent(OperationType.TRANSFER, null, categorySummary(), transferDetails(), null));
        assertThrows(IllegalArgumentException.class,
            () -> recent(OperationType.TRANSFER, null, null, null, null));
    }

    @Test
    void recentActivityItemRejectsNullRequiredFields() {
        AccountSummary account = accountSummary();
        CategorySummary category = categorySummary();

        assertThrows(NullPointerException.class, () -> new RecentActivityItem(null, OperationType.INCOME,
            cents(100), TODAY, account, category, null, null));
        assertThrows(NullPointerException.class, () -> new RecentActivityItem(UUID.randomUUID(), null,
            cents(100), TODAY, account, category, null, null));
        assertThrows(NullPointerException.class, () -> new RecentActivityItem(UUID.randomUUID(),
            OperationType.INCOME, null, TODAY, account, category, null, null));
        assertThrows(NullPointerException.class, () -> new RecentActivityItem(UUID.randomUUID(),
            OperationType.INCOME, cents(100), null, account, category, null, null));
    }

    private static Dashboard dashboard(List<AttentionItem> attention, List<UpcomingCommitment> upcoming,
                                       List<RecentActivityItem> recent) {
        return new Dashboard(new DashboardHorizon(TODAY, TODAY.plusDays(13)), availableToSpend(), attention,
            upcoming, recent);
    }

    @Test
    void dashboardAcceptsEmptyLists() {
        Dashboard dashboard = dashboard(List.of(), List.of(), List.of());

        assertEquals(List.of(), dashboard.attention());
        assertEquals(List.of(), dashboard.upcomingCommitments());
        assertEquals(List.of(), dashboard.recent());
    }

    @Test
    void dashboardRejectsNulls() {
        DashboardHorizon horizon = new DashboardHorizon(TODAY, TODAY);

        assertThrows(NullPointerException.class,
            () -> new Dashboard(null, availableToSpend(), List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class,
            () -> new Dashboard(horizon, null, List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class,
            () -> new Dashboard(horizon, availableToSpend(), null, List.of(), List.of()));
        assertThrows(NullPointerException.class,
            () -> new Dashboard(horizon, availableToSpend(), List.of(), null, List.of()));
        assertThrows(NullPointerException.class,
            () -> new Dashboard(horizon, availableToSpend(), List.of(), List.of(), null));
    }

    @Test
    void dashboardRejectsNullElements() {
        assertThrows(NullPointerException.class,
            () -> dashboard(Arrays.asList((AttentionItem) null), List.of(), List.of()));
        assertThrows(NullPointerException.class,
            () -> dashboard(List.of(), Arrays.asList((UpcomingCommitment) null), List.of()));
        assertThrows(NullPointerException.class,
            () -> dashboard(List.of(), List.of(), Arrays.asList((RecentActivityItem) null)));
    }

    @Test
    void dashboardListsAreImmutableCopies() {
        List<AttentionItem> attention = new ArrayList<>(List.of(new AttentionItem.Shortfall(cents(100))));
        List<UpcomingCommitment> upcoming = new ArrayList<>();
        List<RecentActivityItem> recent = new ArrayList<>();
        Dashboard dashboard = dashboard(attention, upcoming, recent);

        attention.add(new AttentionItem.Shortfall(cents(200)));
        upcoming.add(new UpcomingCommitment(obligationSummary(), TODAY, cents(100), accountSummary(), false));
        recent.add(recent(OperationType.INCOME, accountSummary(), categorySummary(), null, null));

        assertEquals(1, dashboard.attention().size());
        assertEquals(0, dashboard.upcomingCommitments().size());
        assertEquals(0, dashboard.recent().size());
        assertThrows(UnsupportedOperationException.class,
            () -> dashboard.attention().add(new AttentionItem.Shortfall(cents(1))));
        assertThrows(UnsupportedOperationException.class, () -> dashboard.upcomingCommitments().clear());
        assertThrows(UnsupportedOperationException.class, () -> dashboard.recent().clear());
    }
}
