package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.ObligationSummary;
import com.rauldev.personalfinance.application.readmodel.RecentActivityItem;
import com.rauldev.personalfinance.application.readmodel.TransferDetails;
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
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcDashboardQueryAdapterTest {
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();
    private static final UUID ACCOUNT_A1 = id(101);
    private static final UUID ACCOUNT_A2 = id(102);
    private static final UUID ACCOUNT_B1 = id(201);
    private static final UUID ACCOUNT_B2 = id(202);
    private static final UUID CATEGORY_A_INCOME = id(111);
    private static final UUID CATEGORY_A_EXPENSE = id(112);
    private static final UUID CATEGORY_B = id(211);
    private static final LocalDate DATE = LocalDate.of(2026, 10, 4);

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcDashboardQueryAdapter adapter;

    private static UUID id(long value) {
        return new UUID(0, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        connectionProvider = new SQLiteConnectionProvider("jdbc:sqlite:" + tempDir.resolve("test.db").toAbsolutePath());
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        adapter = new JdbcDashboardQueryAdapter(connectionProvider, connectionHolder);

        try (Connection connection = connectionProvider.getConnection()) {
            initializeSchema(connection);
        }
        exec("INSERT INTO users (id) VALUES ('" + USER_A + "')");
        exec("INSERT INTO users (id) VALUES ('" + USER_B + "')");
        insertAccount(ACCOUNT_A1, USER_A, "Checking", 1000, "ACTIVE");
        insertAccount(ACCOUNT_A2, USER_A, "Savings", 2000, "ACTIVE");
        insertAccount(ACCOUNT_B1, USER_B, "B Checking", 5000, "ACTIVE");
        insertAccount(ACCOUNT_B2, USER_B, "B Savings", 6000, "ACTIVE");
        insertCategory(CATEGORY_A_INCOME, USER_A, "Salary", "INCOME", "ACTIVE");
        insertCategory(CATEGORY_A_EXPENSE, USER_A, "Groceries", "EXPENSE", "ACTIVE");
        insertCategory(CATEGORY_B, USER_B, "B Category", "EXPENSE", "ACTIVE");
    }

    // ---- obligations ----

    @Test
    void findActiveObligationsRoundTripsEveryFieldForEachFrequencyWithAndWithoutEndDate() {
        long counter = 1;
        for (Frequency frequency : Frequency.values()) {
            insertObligation(id(1000 + counter), USER_A, "Open " + frequency, 12345 + counter, ACCOUNT_A1,
                CATEGORY_A_EXPENSE, frequency.name(), "2026-01-31", null, "ACTIVE");
            insertObligation(id(2000 + counter), USER_A, "Closed " + frequency, 777 + counter, ACCOUNT_A2,
                CATEGORY_A_INCOME, frequency.name(), "2026-01-31", "2027-12-31", "ACTIVE");
            counter++;
        }

        List<ObligationSnapshot> result = adapter.findActiveObligations(USER_A);

        assertEquals(10, result.size());
        counter = 1;
        for (Frequency frequency : Frequency.values()) {
            ObligationSnapshot open = byId(result, id(1000 + counter));
            assertEquals("Open " + frequency, open.name());
            assertEquals(Money.ofCents(12345 + counter), open.amount());
            assertEquals(ACCOUNT_A1, open.accountId());
            assertEquals(CATEGORY_A_EXPENSE, open.categoryId());
            assertEquals(new Recurrence(frequency, LocalDate.of(2026, 1, 31), null), open.recurrence());
            assertTrue(open.recurrence().endDate().isEmpty());
            assertEquals(ObligationStatus.ACTIVE, open.status());

            ObligationSnapshot closed = byId(result, id(2000 + counter));
            assertEquals(Money.ofCents(777 + counter), closed.amount());
            assertEquals(ACCOUNT_A2, closed.accountId());
            assertEquals(CATEGORY_A_INCOME, closed.categoryId());
            assertEquals(new Recurrence(frequency, LocalDate.of(2026, 1, 31), LocalDate.of(2027, 12, 31)),
                closed.recurrence());
            counter++;
        }
    }

    @Test
    void findActiveObligationsExcludesArchivedObligationsAndOtherUsersObligations() {
        insertObligation(id(1), USER_A, "Active", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "MONTHLY", "2026-10-01", null,
            "ACTIVE");
        insertObligation(id(2), USER_A, "Archived", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "MONTHLY", "2026-10-01",
            null, "ARCHIVED");
        insertObligation(id(3), USER_B, "Other user", 100, ACCOUNT_B1, CATEGORY_B, "MONTHLY", "2026-10-01", null,
            "ACTIVE");

        List<ObligationSnapshot> result = adapter.findActiveObligations(USER_A);

        assertEquals(List.of(id(1)), result.stream().map(ObligationSnapshot::id).toList());
    }

    @Test
    void findActiveObligationsOrdersByNameThenId() {
        insertObligation(id(3), USER_A, "Bbb", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "ONCE", "2026-10-01", null,
            "ACTIVE");
        insertObligation(id(2), USER_A, "Aaa", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "ONCE", "2026-10-01", null,
            "ACTIVE");
        insertObligation(id(1), USER_A, "Ccc", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "ONCE", "2026-10-01", null,
            "ACTIVE");

        List<ObligationSnapshot> result = adapter.findActiveObligations(USER_A);

        assertEquals(List.of(id(2), id(3), id(1)), result.stream().map(ObligationSnapshot::id).toList());
    }

    @Test
    void findActiveObligationsReturnsEmptyListWhenUserHasNone() {
        assertEquals(List.of(), adapter.findActiveObligations(USER_A));
    }

    // ---- resolutions ----

    @Test
    void findResolutionsReturnsPaidAndSkippedResolutionsOfActiveObligations() {
        insertObligation(id(1), USER_A, "Rent", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "MONTHLY", "2026-08-01", null,
            "ACTIVE");
        insertExpense(id(50), ACCOUNT_A1, CATEGORY_A_EXPENSE, 100, "2026-08-01", "ACTIVE", "2026-08-01T10:00:00.000Z");
        insertResolution(id(10), id(1), "2026-08-01", "PAID", id(50));
        insertResolution(id(11), id(1), "2026-09-01", "SKIPPED", null);

        List<ResolutionSnapshot> result = adapter.findResolutionsOfActiveObligations(USER_A);

        assertEquals(List.of(new ResolutionSnapshot(id(1), LocalDate.of(2026, 8, 1)),
            new ResolutionSnapshot(id(1), LocalDate.of(2026, 9, 1))), result);
    }

    @Test
    void findResolutionsExcludesArchivedObligationsAndOtherUsers() {
        insertObligation(id(1), USER_A, "Active", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "MONTHLY", "2026-08-01", null,
            "ACTIVE");
        insertObligation(id(2), USER_A, "Archived", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "MONTHLY", "2026-08-01",
            null, "ARCHIVED");
        insertObligation(id(3), USER_B, "Other", 100, ACCOUNT_B1, CATEGORY_B, "MONTHLY", "2026-08-01", null,
            "ACTIVE");
        insertResolution(id(10), id(1), "2026-08-01", "SKIPPED", null);
        insertResolution(id(11), id(2), "2026-08-01", "SKIPPED", null);
        insertResolution(id(12), id(3), "2026-08-01", "SKIPPED", null);

        List<ResolutionSnapshot> result = adapter.findResolutionsOfActiveObligations(USER_A);

        assertEquals(List.of(new ResolutionSnapshot(id(1), LocalDate.of(2026, 8, 1))), result);
    }

    @Test
    void findResolutionsOrdersByObligationIdThenDueDate() {
        insertObligation(id(2), USER_A, "Second", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "MONTHLY", "2026-08-01", null,
            "ACTIVE");
        insertObligation(id(1), USER_A, "First", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "MONTHLY", "2026-08-01", null,
            "ACTIVE");
        insertResolution(id(10), id(2), "2026-08-01", "SKIPPED", null);
        insertResolution(id(11), id(1), "2026-09-01", "SKIPPED", null);
        insertResolution(id(12), id(1), "2026-08-01", "SKIPPED", null);

        List<ResolutionSnapshot> result = adapter.findResolutionsOfActiveObligations(USER_A);

        assertEquals(List.of(
            new ResolutionSnapshot(id(1), LocalDate.of(2026, 8, 1)),
            new ResolutionSnapshot(id(1), LocalDate.of(2026, 9, 1)),
            new ResolutionSnapshot(id(2), LocalDate.of(2026, 8, 1))), result);
    }

    // ---- accounts ----

    @Test
    void findAccountsRoundTripsEveryFieldIncludingInactiveAccountsAndExcludesOtherUsers() {
        insertAccount(id(103), USER_A, "Old card", 0, "INACTIVE");

        List<AccountSnapshot> result = adapter.findAccounts(USER_A);

        assertEquals(List.of(
            new AccountSnapshot(ACCOUNT_A1, "Checking", Money.ofCents(1000), AccountStatus.ACTIVE),
            new AccountSnapshot(id(103), "Old card", Money.ofCents(0), AccountStatus.INACTIVE),
            new AccountSnapshot(ACCOUNT_A2, "Savings", Money.ofCents(2000), AccountStatus.ACTIVE)), result);
    }

    @Test
    void findAccountsOrdersByNameThenId() {
        insertAccount(id(1), USER_A, "Zzz", 0, "ACTIVE");
        insertAccount(id(2), USER_A, "Aaa", 0, "ACTIVE");

        List<AccountSnapshot> result = adapter.findAccounts(USER_A);

        assertEquals(List.of(id(2), ACCOUNT_A1, ACCOUNT_A2, id(1)),
            result.stream().map(AccountSnapshot::id).toList());
    }

    // ---- categories ----

    @Test
    void findCategoriesReturnsActiveAndInactiveCategoriesOfTheUserOnly() {
        insertCategory(id(113), USER_A, "Old", "EXPENSE", "INACTIVE");

        Set<CategorySnapshot> result = Set.copyOf(adapter.findCategories(USER_A));

        assertEquals(Set.of(
            new CategorySnapshot(CATEGORY_A_INCOME, CategoryStatus.ACTIVE),
            new CategorySnapshot(CATEGORY_A_EXPENSE, CategoryStatus.ACTIVE),
            new CategorySnapshot(id(113), CategoryStatus.INACTIVE)), result);
    }

    // ---- recent activity ----

    @Test
    void findRecentActivityRoundTripsIncomeFields() {
        insertIncome(id(1), ACCOUNT_A1, CATEGORY_A_INCOME, 15050, "2026-10-02", "ACTIVE", "2026-10-02T08:00:00.000Z");

        List<RecentActivityItem> result = adapter.findRecentActivity(USER_A, 10);

        assertEquals(1, result.size());
        RecentActivityItem item = result.get(0);
        assertEquals(id(1), item.operationId());
        assertEquals(OperationType.INCOME, item.operationType());
        assertEquals(Money.ofCents(15050), item.amount());
        assertEquals(LocalDate.of(2026, 10, 2), item.operationDate());
        assertEquals(new AccountSummary(ACCOUNT_A1, "Checking"), item.account());
        assertEquals(new CategorySummary(CATEGORY_A_INCOME, "Salary"), item.category());
        assertNull(item.transfer());
        assertNull(item.obligation());
    }

    @Test
    void findRecentActivityRoundTripsExpenseFieldsWithoutObligation() {
        insertExpense(id(2), ACCOUNT_A2, CATEGORY_A_EXPENSE, 999, "2026-10-03", "ACTIVE", "2026-10-03T08:00:00.000Z");

        List<RecentActivityItem> result = adapter.findRecentActivity(USER_A, 10);

        assertEquals(1, result.size());
        RecentActivityItem item = result.get(0);
        assertEquals(id(2), item.operationId());
        assertEquals(OperationType.EXPENSE, item.operationType());
        assertEquals(Money.ofCents(999), item.amount());
        assertEquals(LocalDate.of(2026, 10, 3), item.operationDate());
        assertEquals(new AccountSummary(ACCOUNT_A2, "Savings"), item.account());
        assertEquals(new CategorySummary(CATEGORY_A_EXPENSE, "Groceries"), item.category());
        assertNull(item.transfer());
        assertNull(item.obligation());
    }

    @Test
    void findRecentActivityRoundTripsTransferFieldsWithSourceAndTargetNames() {
        insertTransfer(id(3), ACCOUNT_A1, ACCOUNT_A2, 500, "2026-10-04", "2026-10-04T08:00:00.000Z");

        List<RecentActivityItem> result = adapter.findRecentActivity(USER_A, 10);

        assertEquals(1, result.size());
        RecentActivityItem item = result.get(0);
        assertEquals(id(3), item.operationId());
        assertEquals(OperationType.TRANSFER, item.operationType());
        assertEquals(Money.ofCents(500), item.amount());
        assertEquals(LocalDate.of(2026, 10, 4), item.operationDate());
        assertEquals(new TransferDetails(new AccountSummary(ACCOUNT_A1, "Checking"),
            new AccountSummary(ACCOUNT_A2, "Savings")), item.transfer());
        assertNull(item.account());
        assertNull(item.category());
        assertNull(item.obligation());
    }

    @Test
    void findRecentActivityLinksAPaidExpenseToItsObligationButNeverASkippedResolution() {
        insertObligation(id(1), USER_A, "Rent", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "MONTHLY", "2026-08-01", null,
            "ACTIVE");
        insertExpense(id(50), ACCOUNT_A1, CATEGORY_A_EXPENSE, 100, "2026-10-01", "ACTIVE", "2026-10-01T10:00:00.000Z");
        insertExpense(id(51), ACCOUNT_A1, CATEGORY_A_EXPENSE, 100, "2026-09-01", "ACTIVE", "2026-09-01T10:00:00.000Z");
        insertResolution(id(10), id(1), "2026-10-01", "PAID", id(50));
        insertResolution(id(11), id(1), "2026-09-01", "SKIPPED", null);

        List<RecentActivityItem> result = adapter.findRecentActivity(USER_A, 10);

        assertEquals(List.of(id(50), id(51)), result.stream().map(RecentActivityItem::operationId).toList());
        assertEquals(new ObligationSummary(id(1), "Rent"), result.get(0).obligation());
        assertNull(result.get(1).obligation());
    }

    @Test
    void findRecentActivityIgnoresAnObligationOwnedByAnotherUserLinkedToTheExpense() {
        insertObligation(id(1), USER_B, "B Secret Rent", 100, ACCOUNT_B1, CATEGORY_B, "MONTHLY", "2026-08-01", null,
            "ACTIVE");
        insertExpense(id(50), ACCOUNT_A1, CATEGORY_A_EXPENSE, 100, "2026-10-01", "ACTIVE", "2026-10-01T10:00:00.000Z");
        insertResolution(id(10), id(1), "2026-10-01", "PAID", id(50));

        List<RecentActivityItem> result = assertDoesNotThrow(() -> adapter.findRecentActivity(USER_A, 10));

        assertEquals(List.of(id(50)), result.stream().map(RecentActivityItem::operationId).toList());
        assertNull(result.get(0).obligation());
        assertFalse(result.toString().contains("B Secret Rent"));
    }

    @Test
    void findRecentActivityOrdersByOperationDateDescendingAcrossOperationTypes() {
        insertIncome(id(1), ACCOUNT_A1, CATEGORY_A_INCOME, 100, "2026-10-01", "ACTIVE", "2026-10-09T00:00:00.000Z");
        insertTransfer(id(2), ACCOUNT_A1, ACCOUNT_A2, 100, "2026-10-03", "2026-10-01T00:00:00.000Z");
        insertExpense(id(3), ACCOUNT_A1, CATEGORY_A_EXPENSE, 100, "2026-10-02", "ACTIVE", "2026-10-05T00:00:00.000Z");

        List<RecentActivityItem> result = adapter.findRecentActivity(USER_A, 10);

        assertEquals(List.of(id(2), id(3), id(1)), result.stream().map(RecentActivityItem::operationId).toList());
    }

    @Test
    void findRecentActivityBreaksDateTiesByCreatedAtDescendingEvenWhenIdsDisagree() {
        insertIncome(id(9), ACCOUNT_A1, CATEGORY_A_INCOME, 100, "2026-10-02", "ACTIVE", "2026-10-02T08:00:00.000Z");
        insertExpense(id(1), ACCOUNT_A1, CATEGORY_A_EXPENSE, 100, "2026-10-02", "ACTIVE", "2026-10-02T09:00:00.000Z");
        insertTransfer(id(5), ACCOUNT_A1, ACCOUNT_A2, 100, "2026-10-02", "2026-10-02T10:00:00.000Z");

        List<RecentActivityItem> result = adapter.findRecentActivity(USER_A, 10);

        assertEquals(List.of(id(5), id(1), id(9)), result.stream().map(RecentActivityItem::operationId).toList());
    }

    @Test
    void findRecentActivityBreaksDateAndCreatedAtTiesByIdDescending() {
        String createdAt = "2026-10-02T08:00:00.000Z";
        insertIncome(id(2), ACCOUNT_A1, CATEGORY_A_INCOME, 100, "2026-10-02", "ACTIVE", createdAt);
        insertExpense(id(3), ACCOUNT_A1, CATEGORY_A_EXPENSE, 100, "2026-10-02", "ACTIVE", createdAt);
        insertTransfer(id(1), ACCOUNT_A1, ACCOUNT_A2, 100, "2026-10-02", createdAt);

        List<RecentActivityItem> result = adapter.findRecentActivity(USER_A, 10);

        assertEquals(List.of(id(3), id(2), id(1)), result.stream().map(RecentActivityItem::operationId).toList());
    }

    @Test
    void findRecentActivityRespectsTheLimitKeepingTheNewestItems() {
        for (int i = 1; i <= 6; i++) {
            insertIncome(id(i), ACCOUNT_A1, CATEGORY_A_INCOME, 100, "2026-10-0" + i, "ACTIVE",
                "2026-10-0" + i + "T08:00:00.000Z");
        }

        List<RecentActivityItem> five = adapter.findRecentActivity(USER_A, 5);
        List<RecentActivityItem> one = adapter.findRecentActivity(USER_A, 1);
        List<RecentActivityItem> many = adapter.findRecentActivity(USER_A, 100);

        assertEquals(List.of(id(6), id(5), id(4), id(3), id(2)),
            five.stream().map(RecentActivityItem::operationId).toList());
        assertEquals(List.of(id(6)), one.stream().map(RecentActivityItem::operationId).toList());
        assertEquals(6, many.size());
    }

    @Test
    void findRecentActivityExcludesCancelledIncomeAndExpense() {
        insertIncome(id(1), ACCOUNT_A1, CATEGORY_A_INCOME, 100, "2026-10-01", "CANCELLED", "2026-10-01T08:00:00.000Z");
        insertExpense(id(2), ACCOUNT_A1, CATEGORY_A_EXPENSE, 100, "2026-10-02", "CANCELLED", "2026-10-02T08:00:00.000Z");
        insertIncome(id(3), ACCOUNT_A1, CATEGORY_A_INCOME, 100, "2026-10-03", "ACTIVE", "2026-10-03T08:00:00.000Z");

        List<RecentActivityItem> result = adapter.findRecentActivity(USER_A, 10);

        assertEquals(List.of(id(3)), result.stream().map(RecentActivityItem::operationId).toList());
    }

    @Test
    void findRecentActivityExcludesOtherUsersOperationsIncludingTransfers() {
        insertIncome(id(1), ACCOUNT_A1, CATEGORY_A_INCOME, 100, "2026-10-01", "ACTIVE", "2026-10-01T08:00:00.000Z");
        insertIncome(id(2), ACCOUNT_B1, CATEGORY_B, 100, "2026-10-02", "ACTIVE", "2026-10-02T08:00:00.000Z");
        insertExpense(id(3), ACCOUNT_B1, CATEGORY_B, 100, "2026-10-03", "ACTIVE", "2026-10-03T08:00:00.000Z");
        insertTransfer(id(4), ACCOUNT_B1, ACCOUNT_B2, 100, "2026-10-04", "2026-10-04T08:00:00.000Z");

        List<RecentActivityItem> forA = adapter.findRecentActivity(USER_A, 10);
        List<RecentActivityItem> forB = adapter.findRecentActivity(USER_B, 10);

        assertEquals(List.of(id(1)), forA.stream().map(RecentActivityItem::operationId).toList());
        assertEquals(List.of(id(4), id(3), id(2)), forB.stream().map(RecentActivityItem::operationId).toList());
    }

    @Test
    void findRecentActivityIncludesOperationsOfInactiveAccountsAndCategories() {
        insertAccount(id(103), USER_A, "Old card", 0, "INACTIVE");
        insertCategory(id(113), USER_A, "Old", "EXPENSE", "INACTIVE");
        insertExpense(id(1), id(103), id(113), 100, "2026-10-01", "ACTIVE", "2026-10-01T08:00:00.000Z");

        List<RecentActivityItem> result = adapter.findRecentActivity(USER_A, 10);

        assertEquals(List.of(id(1)), result.stream().map(RecentActivityItem::operationId).toList());
    }

    @Test
    void findRecentActivityRejectsLimitBelowOneBeforeOpeningAnyConnection() {
        JdbcDashboardQueryAdapter unreachable = adapterWithUnreachableDatabase();

        assertThrows(RuntimeException.class, () -> unreachable.findAccounts(USER_A));
        assertThrows(IllegalArgumentException.class, () -> unreachable.findRecentActivity(USER_A, 0));
        assertThrows(IllegalArgumentException.class, () -> unreachable.findRecentActivity(USER_A, -1));
    }

    // ---- null arguments ----

    @Test
    void rejectsNullUserIds() {
        assertThrows(NullPointerException.class, () -> adapter.findActiveObligations(null));
        assertThrows(NullPointerException.class, () -> adapter.findResolutionsOfActiveObligations(null));
        assertThrows(NullPointerException.class, () -> adapter.findAccounts(null));
        assertThrows(NullPointerException.class, () -> adapter.findCategories(null));
        assertThrows(NullPointerException.class, () -> adapter.findRecentActivity(null, 5));
    }

    @Test
    void constructorRejectsNullDependencies() {
        assertThrows(NullPointerException.class, () -> new JdbcDashboardQueryAdapter(null, connectionHolder));
        assertThrows(NullPointerException.class, () -> new JdbcDashboardQueryAdapter(connectionProvider, null));
    }

    // ---- connection modes ----

    @Test
    void worksOutsideAnActiveTransactionWithoutLeavingAConnectionBound() {
        insertAccount(id(103), USER_A, "Extra", 0, "ACTIVE");

        List<AccountSnapshot> accounts = adapter.findAccounts(USER_A);

        assertEquals(3, accounts.size());
        assertFalse(connectionHolder.hasActiveTransaction());
    }

    @Test
    void worksInsideAnActiveTransactionReusingAndNotClosingTheBoundConnection() {
        Boolean stillOpen = transactionManager.execute(() -> {
            try {
                Connection bound = connectionHolder.get();
                try (Statement statement = bound.createStatement()) {
                    statement.execute("INSERT INTO accounts (id, user_id, name, balance, status) VALUES ('"
                        + id(103) + "', '" + USER_A + "', 'Uncommitted', 0, 'ACTIVE')");
                }

                List<AccountSnapshot> first = adapter.findAccounts(USER_A);
                List<ObligationSnapshot> second = adapter.findActiveObligations(USER_A);

                assertTrue(first.stream().anyMatch(account -> account.id().equals(id(103))));
                assertEquals(List.of(), second);
                assertTrue(connectionHolder.hasActiveTransaction());
                return !bound.isClosed();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });

        assertTrue(stillOpen);
        assertFalse(connectionHolder.hasActiveTransaction());
    }

    @Test
    void wrapsSqlFailuresWithTheFailedPartAndTheOriginalCause() throws Exception {
        SQLiteConnectionProvider emptyDatabase = new SQLiteConnectionProvider(
            "jdbc:sqlite:" + tempDir.resolve("no-schema.db").toAbsolutePath());
        JdbcDashboardQueryAdapter broken = new JdbcDashboardQueryAdapter(emptyDatabase, connectionHolder);

        RuntimeException obligations = assertThrows(RuntimeException.class, () -> broken.findActiveObligations(USER_A));
        RuntimeException resolutions = assertThrows(RuntimeException.class,
            () -> broken.findResolutionsOfActiveObligations(USER_A));
        RuntimeException accounts = assertThrows(RuntimeException.class, () -> broken.findAccounts(USER_A));
        RuntimeException categories = assertThrows(RuntimeException.class, () -> broken.findCategories(USER_A));
        RuntimeException recent = assertThrows(RuntimeException.class, () -> broken.findRecentActivity(USER_A, 5));

        assertEquals("Failed to query dashboard obligations", obligations.getMessage());
        assertEquals("Failed to query dashboard resolutions", resolutions.getMessage());
        assertEquals("Failed to query dashboard accounts", accounts.getMessage());
        assertEquals("Failed to query dashboard categories", categories.getMessage());
        assertEquals("Failed to query dashboard recent activity", recent.getMessage());
        for (RuntimeException e : List.of(obligations, resolutions, accounts, categories, recent)) {
            assertInstanceOf(SQLException.class, e.getCause());
        }
    }

    // ---- corrupt rows ----

    @Test
    void findActiveObligationsFailsWithCorruptedPersistedDataWhenFrequencyIsInvalid() throws SQLException {
        insertObligation(id(77), USER_A, "Broken", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "BOGUS", "2026-10-01", null,
            "ACTIVE");

        CorruptedPersistedDataException e = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findActiveObligations(USER_A));

        assertTrue(e.getMessage().contains("obligations"));
        assertTrue(e.getMessage().contains(id(77).toString()));
        assertInstanceOf(IllegalArgumentException.class, e.getCause());
    }

    @Test
    void findActiveObligationsFailsWithCorruptedPersistedDataWhenEndDateIsBeforeStartDate() throws SQLException {
        execIgnoringChecks("INSERT INTO obligations (id, user_id, name, amount, account_id, category_id, frequency, "
            + "start_date, end_date, status) VALUES ('" + id(78) + "', '" + USER_A + "', 'Broken', 100, '"
            + ACCOUNT_A1 + "', '" + CATEGORY_A_EXPENSE + "', 'MONTHLY', '2026-10-01', '2026-09-01', 'ACTIVE')");

        CorruptedPersistedDataException e = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findActiveObligations(USER_A));

        assertTrue(e.getMessage().contains("obligations"));
        assertTrue(e.getMessage().contains(id(78).toString()));
        assertInstanceOf(IllegalArgumentException.class, e.getCause());
    }

    @Test
    void findActiveObligationsFailsWithCorruptedPersistedDataWhenAmountIsNotPositive() throws SQLException {
        execIgnoringChecks("INSERT INTO obligations (id, user_id, name, amount, account_id, category_id, frequency, "
            + "start_date, end_date, status) VALUES ('" + id(79) + "', '" + USER_A + "', 'Broken', 0, '"
            + ACCOUNT_A1 + "', '" + CATEGORY_A_EXPENSE + "', 'MONTHLY', '2026-10-01', NULL, 'ACTIVE')");

        CorruptedPersistedDataException e = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findActiveObligations(USER_A));

        assertTrue(e.getMessage().contains(id(79).toString()));
        assertInstanceOf(IllegalArgumentException.class, e.getCause());
    }

    @Test
    void findResolutionsFailsWithCorruptedPersistedDataWhenDueDateIsInvalid() throws SQLException {
        insertObligation(id(1), USER_A, "Rent", 100, ACCOUNT_A1, CATEGORY_A_EXPENSE, "MONTHLY", "2026-08-01", null,
            "ACTIVE");
        insertResolution(id(88), id(1), "not-a-date", "SKIPPED", null);

        CorruptedPersistedDataException e = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findResolutionsOfActiveObligations(USER_A));

        assertTrue(e.getMessage().contains("occurrence_resolutions"));
        assertTrue(e.getMessage().contains(id(88).toString()));
        assertInstanceOf(DateTimeParseException.class, e.getCause());
    }

    @Test
    void findAccountsFailsWithCorruptedPersistedDataWhenStatusIsInvalid() throws SQLException {
        insertAccount(id(66), USER_A, "Broken", 0, "BOGUS");

        CorruptedPersistedDataException e = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findAccounts(USER_A));

        assertTrue(e.getMessage().contains("accounts"));
        assertTrue(e.getMessage().contains(id(66).toString()));
        assertInstanceOf(IllegalArgumentException.class, e.getCause());
    }

    @Test
    void findCategoriesFailsWithCorruptedPersistedDataWhenStatusIsInvalid() throws SQLException {
        insertCategory(id(67), USER_A, "Broken", "EXPENSE", "BOGUS");

        CorruptedPersistedDataException e = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findCategories(USER_A));

        assertTrue(e.getMessage().contains("categories"));
        assertTrue(e.getMessage().contains(id(67).toString()));
        assertInstanceOf(IllegalArgumentException.class, e.getCause());
    }

    @Test
    void findRecentActivityFailsWithCorruptedPersistedDataWhenAnIncomeDateIsInvalid() throws SQLException {
        insertIncome(id(68), ACCOUNT_A1, CATEGORY_A_INCOME, 100, "not-a-date", "ACTIVE", "2026-10-01T08:00:00.000Z");

        CorruptedPersistedDataException e = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findRecentActivity(USER_A, 5));

        assertTrue(e.getMessage().contains("financial operations (INCOME)"));
        assertTrue(e.getMessage().contains(id(68).toString()));
        assertInstanceOf(DateTimeParseException.class, e.getCause());
    }

    @Test
    void findRecentActivityFailsWithCorruptedPersistedDataWhenAnExpenseDateIsInvalid() throws SQLException {
        insertExpense(id(69), ACCOUNT_A1, CATEGORY_A_EXPENSE, 100, "not-a-date", "ACTIVE", "2026-10-01T08:00:00.000Z");

        CorruptedPersistedDataException e = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findRecentActivity(USER_A, 5));

        assertTrue(e.getMessage().contains("financial operations (EXPENSE)"));
        assertTrue(e.getMessage().contains(id(69).toString()));
        assertInstanceOf(DateTimeParseException.class, e.getCause());
    }

    @Test
    void findRecentActivityFailsWithCorruptedPersistedDataWhenATransferDateIsInvalid() throws SQLException {
        insertTransfer(id(70), ACCOUNT_A1, ACCOUNT_A2, 100, "not-a-date", "2026-10-01T08:00:00.000Z");

        CorruptedPersistedDataException e = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findRecentActivity(USER_A, 5));

        assertTrue(e.getMessage().contains("financial operations (TRANSFER)"));
        assertTrue(e.getMessage().contains(id(70).toString()));
        assertInstanceOf(DateTimeParseException.class, e.getCause());
    }

    // ---- helpers ----

    private JdbcDashboardQueryAdapter adapterWithUnreachableDatabase() {
        SQLiteConnectionProvider unreachable = new SQLiteConnectionProvider(
            "jdbc:sqlite:" + tempDir.resolve("missing-directory").resolve("db.sqlite").toAbsolutePath());
        return new JdbcDashboardQueryAdapter(unreachable, connectionHolder);
    }

    private static ObligationSnapshot byId(List<ObligationSnapshot> obligations, UUID id) {
        return obligations.stream().filter(obligation -> obligation.id().equals(id)).collect(Collectors.toList())
            .get(0);
    }

    private void insertAccount(UUID id, UUID userId, String name, long balance, String status) {
        exec("INSERT INTO accounts (id, user_id, name, balance, status) VALUES ('" + id + "', '" + userId + "', '"
            + name + "', " + balance + ", '" + status + "')");
    }

    private void insertCategory(UUID id, UUID userId, String name, String type, String status) {
        exec("INSERT INTO categories (id, user_id, name, type, status) VALUES ('" + id + "', '" + userId + "', '"
            + name + "', '" + type + "', '" + status + "')");
    }

    private void insertObligation(UUID id, UUID userId, String name, long amount, UUID accountId, UUID categoryId,
                                  String frequency, String startDate, String endDate, String status) {
        String end = endDate == null ? "NULL" : "'" + endDate + "'";
        exec("INSERT INTO obligations (id, user_id, name, amount, account_id, category_id, frequency, start_date, "
            + "end_date, status) VALUES ('" + id + "', '" + userId + "', '" + name + "', " + amount + ", '"
            + accountId + "', '" + categoryId + "', '" + frequency + "', '" + startDate + "', " + end + ", '"
            + status + "')");
    }

    private void insertResolution(UUID id, UUID obligationId, String dueDate, String status, UUID expenseId) {
        String expense = expenseId == null ? "NULL" : "'" + expenseId + "'";
        exec("INSERT INTO occurrence_resolutions (id, obligation_id, due_date, status, expense_id, resolved_at) "
            + "VALUES ('" + id + "', '" + obligationId + "', '" + dueDate + "', '" + status + "', " + expense
            + ", '2026-10-01T00:00:00.000Z')");
    }

    private void insertIncome(UUID id, UUID accountId, UUID categoryId, long amount, String date, String status,
                              String createdAt) {
        insertOperation("income_operations", id, accountId, categoryId, amount, date, status, createdAt);
    }

    private void insertExpense(UUID id, UUID accountId, UUID categoryId, long amount, String date, String status,
                               String createdAt) {
        insertOperation("expense_operations", id, accountId, categoryId, amount, date, status, createdAt);
    }

    private void insertOperation(String table, UUID id, UUID accountId, UUID categoryId, long amount, String date,
                                 String status, String createdAt) {
        exec("INSERT INTO " + table + " (id, account_id, category_id, amount, operation_date, status, created_at) "
            + "VALUES ('" + id + "', '" + accountId + "', '" + categoryId + "', " + amount + ", '" + date + "', '"
            + status + "', '" + createdAt + "')");
    }

    private void insertTransfer(UUID id, UUID sourceId, UUID targetId, long amount, String date, String createdAt) {
        exec("INSERT INTO transfer_operations (id, source_account_id, target_account_id, amount, operation_date, "
            + "created_at) VALUES ('" + id + "', '" + sourceId + "', '" + targetId + "', " + amount + ", '" + date
            + "', '" + createdAt + "')");
    }

    private void exec(String sql) {
        try (Connection connection = connectionProvider.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void execIgnoringChecks(String sql) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA ignore_check_constraints = ON");
            statement.execute(sql);
        }
    }

    private void initializeSchema(Connection connection) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("db/schema.sql resource not found on classpath");
            }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(trimmed);
                    }
                }
            }
        }
    }
}
