package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rauldev.personalfinance.application.query.OperationSearchCriteria;
import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationDetails;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationHistoryItem;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.Income;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;
import com.rauldev.personalfinance.domain.Reversal;
import com.rauldev.personalfinance.domain.Transfer;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcFinancialOperationQueryAdapterTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcAccountRepository accountRepository;
    private JdbcCategoryRepository categoryRepository;
    private JdbcIncomeOperationRepository incomeOperationRepository;
    private JdbcExpenseOperationRepository expenseOperationRepository;
    private JdbcTransferOperationRepository transferOperationRepository;
    private JdbcReversalRepository reversalRepository;
    private JdbcFinancialOperationQueryAdapter queryAdapter;

    private static final UUID USER_A_ID = UUID.randomUUID();
    private static final UUID USER_B_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_A1_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_A2_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_B1_ID = UUID.randomUUID();
    private static final UUID CATEGORY_INCOME_ID = UUID.randomUUID();
    private static final UUID CATEGORY_EXPENSE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        accountRepository = new JdbcAccountRepository(connectionHolder);
        categoryRepository = new JdbcCategoryRepository(connectionHolder);
        incomeOperationRepository = new JdbcIncomeOperationRepository(connectionHolder);
        expenseOperationRepository = new JdbcExpenseOperationRepository(connectionHolder);
        transferOperationRepository = new JdbcTransferOperationRepository(connectionHolder);
        reversalRepository = new JdbcReversalRepository(connectionHolder);
        queryAdapter = new JdbcFinancialOperationQueryAdapter(connectionProvider, connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            seedUser(conn, USER_A_ID);
            seedUser(conn, USER_B_ID);
        }

        transactionManager.execute(() -> {
            accountRepository.create(new Account(ACCOUNT_A1_ID, USER_A_ID, "Checking"));
            accountRepository.create(new Account(ACCOUNT_A2_ID, USER_A_ID, "Savings"));
            accountRepository.create(new Account(ACCOUNT_B1_ID, USER_B_ID, "User B Checking"));
            categoryRepository.create(new Category(CATEGORY_INCOME_ID, USER_A_ID, "Salary", CategoryType.INCOME));
            categoryRepository.create(new Category(CATEGORY_EXPENSE_ID, USER_A_ID, "Groceries", CategoryType.EXPENSE));
        });
    }

    @Test
    void search_returnsIncomeOperations() {
        UUID incomeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 2);

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_A_ID, Money.of("1000.00"), date, ACCOUNT_A1_ID, CATEGORY_INCOME_ID);
            incomeOperationRepository.create(income);
        });

        List<FinancialOperationHistoryItem> results = queryAdapter.search(OperationSearchCriteria.forUser(USER_A_ID));

        assertEquals(1, results.size());
        FinancialOperationHistoryItem item = results.get(0);
        assertEquals(incomeId, item.operationId());
        assertEquals(OperationType.INCOME, item.operationType());
        assertEquals(Money.of("1000.00"), item.amount());
        assertEquals(date, item.operationDate());
        assertEquals(OperationStatus.ACTIVE, item.status());
        assertNull(item.cancelledAt());
        assertEquals(ACCOUNT_A1_ID, item.account().id());
        assertEquals("Checking", item.account().name());
        assertEquals(CATEGORY_INCOME_ID, item.category().id());
        assertEquals("Salary", item.category().name());
        assertNull(item.transfer());
    }

    @Test
    void search_returnsExpenseOperations() {
        UUID expenseId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 2);

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_A_ID, Money.of("150.00"), date, ACCOUNT_A1_ID, CATEGORY_EXPENSE_ID);
            expenseOperationRepository.create(expense);
        });

        List<FinancialOperationHistoryItem> results = queryAdapter.search(OperationSearchCriteria.forUser(USER_A_ID));

        assertEquals(1, results.size());
        FinancialOperationHistoryItem item = results.get(0);
        assertEquals(expenseId, item.operationId());
        assertEquals(OperationType.EXPENSE, item.operationType());
        assertEquals(Money.of("150.00"), item.amount());
        assertEquals(date, item.operationDate());
        assertEquals(OperationStatus.ACTIVE, item.status());
        assertNull(item.cancelledAt());
        assertEquals(ACCOUNT_A1_ID, item.account().id());
        assertEquals("Checking", item.account().name());
        assertEquals(CATEGORY_EXPENSE_ID, item.category().id());
        assertEquals("Groceries", item.category().name());
        assertNull(item.transfer());
    }

    @Test
    void search_returnsTransferOperations() {
        UUID transferId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 2);

        transactionManager.execute(() -> {
            Transfer transfer = new Transfer(transferId, USER_A_ID, Money.of("500.00"), date, ACCOUNT_A1_ID, ACCOUNT_A2_ID);
            transferOperationRepository.create(transfer);
        });

        List<FinancialOperationHistoryItem> results = queryAdapter.search(OperationSearchCriteria.forUser(USER_A_ID));

        assertEquals(1, results.size());
        FinancialOperationHistoryItem item = results.get(0);
        assertEquals(transferId, item.operationId());
        assertEquals(OperationType.TRANSFER, item.operationType());
        assertEquals(Money.of("500.00"), item.amount());
        assertEquals(date, item.operationDate());
        assertNull(item.status());
        assertNull(item.cancelledAt());
        assertNull(item.account());
        assertNull(item.category());
        assertNotNull(item.transfer());
        assertEquals(ACCOUNT_A1_ID, item.transfer().sourceAccount().id());
        assertEquals("Checking", item.transfer().sourceAccount().name());
        assertEquals(ACCOUNT_A2_ID, item.transfer().targetAccount().id());
        assertEquals("Savings", item.transfer().targetAccount().name());
    }

    @Test
    void search_combinesDifferentOperationTypesSorted() {
        UUID idLow = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID idHigh = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        transactionManager.execute(() -> {
            incomeOperationRepository.create(
                new Income(idLow, USER_A_ID, Money.of("100.00"), LocalDate.of(2026, 9, 1), ACCOUNT_A1_ID, CATEGORY_INCOME_ID));
            expenseOperationRepository.create(
                new Expense(idHigh, USER_A_ID, Money.of("50.00"), LocalDate.of(2026, 9, 2), ACCOUNT_A1_ID, CATEGORY_EXPENSE_ID));
            transferOperationRepository.create(
                new Transfer(idLow, USER_A_ID, Money.of("200.00"), LocalDate.of(2026, 9, 2), ACCOUNT_A1_ID, ACCOUNT_A2_ID));
        });

        List<FinancialOperationHistoryItem> results = queryAdapter.search(OperationSearchCriteria.forUser(USER_A_ID));

        assertEquals(3, results.size());
        assertEquals(LocalDate.of(2026, 9, 2), results.get(0).operationDate());
        assertEquals(idHigh, results.get(0).operationId());
        assertEquals(LocalDate.of(2026, 9, 2), results.get(1).operationDate());
        assertEquals(idLow, results.get(1).operationId());
        assertEquals(LocalDate.of(2026, 9, 1), results.get(2).operationDate());
    }

    @Test
    void search_appliesUserIsolation() {
        transactionManager.execute(() -> {
            incomeOperationRepository.create(
                new Income(USER_A_ID, Money.of("100.00"), LocalDate.now(), ACCOUNT_A1_ID, CATEGORY_INCOME_ID));
            categoryRepository.create(new Category(USER_B_ID, "B Cat", CategoryType.INCOME));
            incomeOperationRepository.create(
                new Income(USER_B_ID, Money.of("300.00"), LocalDate.now(), ACCOUNT_B1_ID, CATEGORY_INCOME_ID));
        });

        List<FinancialOperationHistoryItem> userAResults = queryAdapter.search(OperationSearchCriteria.forUser(USER_A_ID));
        List<FinancialOperationHistoryItem> userBResults = queryAdapter.search(OperationSearchCriteria.forUser(USER_B_ID));

        assertEquals(1, userAResults.size());
        assertEquals(Money.of("100.00"), userAResults.get(0).amount());

        assertEquals(1, userBResults.size());
        assertEquals(Money.of("300.00"), userBResults.get(0).amount());
    }

    @Test
    void search_filtersByAccount() {
        transactionManager.execute(() -> {
            incomeOperationRepository.create(
                new Income(USER_A_ID, Money.of("100.00"), LocalDate.now(), ACCOUNT_A1_ID, CATEGORY_INCOME_ID));
            incomeOperationRepository.create(
                new Income(USER_A_ID, Money.of("200.00"), LocalDate.now(), ACCOUNT_A2_ID, CATEGORY_INCOME_ID));
            transferOperationRepository.create(
                new Transfer(USER_A_ID, Money.of("50.00"), LocalDate.now(), ACCOUNT_A1_ID, ACCOUNT_A2_ID));
        });

        OperationSearchCriteria criteriaA1 = new OperationSearchCriteria(
            USER_A_ID, ACCOUNT_A1_ID, null, null, null, null, 1, 20);
        List<FinancialOperationHistoryItem> results = queryAdapter.search(criteriaA1);

        assertEquals(2, results.size());
    }

    @Test
    void search_filtersByCategory() {
        transactionManager.execute(() -> {
            incomeOperationRepository.create(
                new Income(USER_A_ID, Money.of("100.00"), LocalDate.now(), ACCOUNT_A1_ID, CATEGORY_INCOME_ID));
            expenseOperationRepository.create(
                new Expense(USER_A_ID, Money.of("50.00"), LocalDate.now(), ACCOUNT_A1_ID, CATEGORY_EXPENSE_ID));
            transferOperationRepository.create(
                new Transfer(USER_A_ID, Money.of("25.00"), LocalDate.now(), ACCOUNT_A1_ID, ACCOUNT_A2_ID));
        });

        OperationSearchCriteria criteria = new OperationSearchCriteria(
            USER_A_ID, null, CATEGORY_INCOME_ID, null, null, null, 1, 20);
        List<FinancialOperationHistoryItem> results = queryAdapter.search(criteria);

        assertEquals(1, results.size());
        assertEquals(OperationType.INCOME, results.get(0).operationType());
    }

    @Test
    void search_filtersByOperationType() {
        transactionManager.execute(() -> {
            incomeOperationRepository.create(
                new Income(USER_A_ID, Money.of("100.00"), LocalDate.now(), ACCOUNT_A1_ID, CATEGORY_INCOME_ID));
            expenseOperationRepository.create(
                new Expense(USER_A_ID, Money.of("50.00"), LocalDate.now(), ACCOUNT_A1_ID, CATEGORY_EXPENSE_ID));
            transferOperationRepository.create(
                new Transfer(USER_A_ID, Money.of("25.00"), LocalDate.now(), ACCOUNT_A1_ID, ACCOUNT_A2_ID));
        });

        OperationSearchCriteria criteria = new OperationSearchCriteria(
            USER_A_ID, null, null, OperationType.TRANSFER, null, null, 1, 20);
        List<FinancialOperationHistoryItem> results = queryAdapter.search(criteria);

        assertEquals(1, results.size());
        assertEquals(OperationType.TRANSFER, results.get(0).operationType());
    }

    @Test
    void search_filtersByDateRange() {
        LocalDate date1 = LocalDate.of(2026, 9, 1);
        LocalDate date2 = LocalDate.of(2026, 9, 5);
        LocalDate date3 = LocalDate.of(2026, 9, 10);

        transactionManager.execute(() -> {
            incomeOperationRepository.create(
                new Income(USER_A_ID, Money.of("100.00"), date1, ACCOUNT_A1_ID, CATEGORY_INCOME_ID));
            incomeOperationRepository.create(
                new Income(USER_A_ID, Money.of("200.00"), date2, ACCOUNT_A1_ID, CATEGORY_INCOME_ID));
            incomeOperationRepository.create(
                new Income(USER_A_ID, Money.of("300.00"), date3, ACCOUNT_A1_ID, CATEGORY_INCOME_ID));
        });

        OperationSearchCriteria criteria = new OperationSearchCriteria(
            USER_A_ID, null, null, null, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 7), 1, 20);
        List<FinancialOperationHistoryItem> results = queryAdapter.search(criteria);

        assertEquals(1, results.size());
        assertEquals(Money.of("200.00"), results.get(0).amount());
    }

    @Test
    void search_appliesPaginationCorrectly() {
        transactionManager.execute(() -> {
            incomeOperationRepository.create(
                new Income(USER_A_ID, Money.of("100.00"), LocalDate.of(2026, 9, 1), ACCOUNT_A1_ID, CATEGORY_INCOME_ID));
            incomeOperationRepository.create(
                new Income(USER_A_ID, Money.of("200.00"), LocalDate.of(2026, 9, 2), ACCOUNT_A1_ID, CATEGORY_INCOME_ID));
            incomeOperationRepository.create(
                new Income(USER_A_ID, Money.of("300.00"), LocalDate.of(2026, 9, 3), ACCOUNT_A1_ID, CATEGORY_INCOME_ID));
        });

        OperationSearchCriteria page1 = new OperationSearchCriteria(
            USER_A_ID, null, null, null, null, null, 1, 2);
        List<FinancialOperationHistoryItem> page1Results = queryAdapter.search(page1);

        assertEquals(2, page1Results.size());
        assertEquals(Money.of("300.00"), page1Results.get(0).amount());
        assertEquals(Money.of("200.00"), page1Results.get(1).amount());

        OperationSearchCriteria page2 = new OperationSearchCriteria(
            USER_A_ID, null, null, null, null, null, 2, 2);
        List<FinancialOperationHistoryItem> page2Results = queryAdapter.search(page2);

        assertEquals(1, page2Results.size());
        assertEquals(Money.of("100.00"), page2Results.get(0).amount());
    }

    @Test
    void search_correctlyRepresentsCancelledOperations() {
        UUID incomeId = UUID.randomUUID();
        Instant cancelledTime = Instant.now();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_A_ID, Money.of("1000.00"), LocalDate.now(), ACCOUNT_A1_ID, CATEGORY_INCOME_ID);
            incomeOperationRepository.create(income);
            income.cancel();
            incomeOperationRepository.update(income);
            reversalRepository.create(new Reversal(income, cancelledTime));
        });

        List<FinancialOperationHistoryItem> results = queryAdapter.search(OperationSearchCriteria.forUser(USER_A_ID));

        assertEquals(1, results.size());
        FinancialOperationHistoryItem item = results.get(0);
        assertEquals(OperationStatus.CANCELLED, item.status());
        assertEquals(cancelledTime, item.cancelledAt());
    }

    @Test
    void search_returnsEmptyListWhenNoMatches() {
        List<FinancialOperationHistoryItem> results = queryAdapter.search(OperationSearchCriteria.forUser(USER_A_ID));

        assertTrue(results.isEmpty());
    }

    @Test
    void findDetailByIdAndUserId_returnsIncomeDetails() {
        UUID incomeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 2);

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_A_ID, Money.of("1000.00"), date, ACCOUNT_A1_ID, CATEGORY_INCOME_ID);
            incomeOperationRepository.create(income);
        });

        Optional<FinancialOperationDetails> details = queryAdapter.findDetailByIdAndUserId(incomeId, USER_A_ID);

        assertTrue(details.isPresent());
        assertEquals(incomeId, details.get().operationId());
        assertEquals(OperationType.INCOME, details.get().operationType());
        assertEquals(Money.of("1000.00"), details.get().amount());
        assertEquals(date, details.get().operationDate());
        assertEquals(OperationStatus.ACTIVE, details.get().status());
        assertNull(details.get().cancelledAt());
        assertEquals(ACCOUNT_A1_ID, details.get().account().id());
        assertEquals(CATEGORY_INCOME_ID, details.get().category().id());
    }

    @Test
    void findDetailByIdAndUserId_returnsExpenseDetails() {
        UUID expenseId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 2);

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_A_ID, Money.of("150.00"), date, ACCOUNT_A1_ID, CATEGORY_EXPENSE_ID);
            expenseOperationRepository.create(expense);
        });

        Optional<FinancialOperationDetails> details = queryAdapter.findDetailByIdAndUserId(expenseId, USER_A_ID);

        assertTrue(details.isPresent());
        assertEquals(expenseId, details.get().operationId());
        assertEquals(OperationType.EXPENSE, details.get().operationType());
        assertEquals(Money.of("150.00"), details.get().amount());
    }

    @Test
    void findDetailByIdAndUserId_returnsTransferDetails() {
        UUID transferId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 2);

        transactionManager.execute(() -> {
            Transfer transfer = new Transfer(transferId, USER_A_ID, Money.of("500.00"), date, ACCOUNT_A1_ID, ACCOUNT_A2_ID);
            transferOperationRepository.create(transfer);
        });

        Optional<FinancialOperationDetails> details = queryAdapter.findDetailByIdAndUserId(transferId, USER_A_ID);

        assertTrue(details.isPresent());
        assertEquals(transferId, details.get().operationId());
        assertEquals(OperationType.TRANSFER, details.get().operationType());
        assertEquals(Money.of("500.00"), details.get().amount());
        assertNotNull(details.get().transfer());
        assertEquals(ACCOUNT_A1_ID, details.get().transfer().sourceAccount().id());
        assertEquals(ACCOUNT_A2_ID, details.get().transfer().targetAccount().id());
    }

    @Test
    void findDetailByIdAndUserId_returnsCancelledOperationDetails() {
        UUID expenseId = UUID.randomUUID();
        Instant cancelledTime = Instant.now();

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_A_ID, Money.of("150.00"), LocalDate.now(), ACCOUNT_A1_ID, CATEGORY_EXPENSE_ID);
            expenseOperationRepository.create(expense);
            expense.cancel();
            expenseOperationRepository.update(expense);
            reversalRepository.create(new Reversal(expense, cancelledTime));
        });

        Optional<FinancialOperationDetails> details = queryAdapter.findDetailByIdAndUserId(expenseId, USER_A_ID);

        assertTrue(details.isPresent());
        assertEquals(OperationStatus.CANCELLED, details.get().status());
        assertEquals(cancelledTime, details.get().cancelledAt());
    }

    @Test
    void findDetailByIdAndUserId_returnsEmptyWhenNotFound() {
        Optional<FinancialOperationDetails> details = queryAdapter.findDetailByIdAndUserId(UUID.randomUUID(), USER_A_ID);

        assertFalse(details.isPresent());
    }

    @Test
    void findDetailByIdAndUserId_returnsEmptyWhenUserIdDoesNotMatch() {
        UUID incomeId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_A_ID, Money.of("1000.00"), LocalDate.now(), ACCOUNT_A1_ID, CATEGORY_INCOME_ID);
            incomeOperationRepository.create(income);
        });

        Optional<FinancialOperationDetails> details = queryAdapter.findDetailByIdAndUserId(incomeId, USER_B_ID);

        assertFalse(details.isPresent());
    }

    @Test
    void queryAdapter_worksOutsideAndInsideActiveTransaction() {
        UUID incomeId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_A_ID, Money.of("1000.00"), LocalDate.now(), ACCOUNT_A1_ID, CATEGORY_INCOME_ID);
            incomeOperationRepository.create(income);
        });

        Optional<FinancialOperationDetails> outsideDetails = queryAdapter.findDetailByIdAndUserId(incomeId, USER_A_ID);
        assertTrue(outsideDetails.isPresent());

        Optional<FinancialOperationDetails> insideDetails = transactionManager.execute(
            () -> queryAdapter.findDetailByIdAndUserId(incomeId, USER_A_ID));
        assertTrue(insideDetails.isPresent());
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

    private void seedUser(Connection connection, UUID userId) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO users (id) VALUES ('" + userId + "')");
        }
    }
}
