package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.Income;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Reversal;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcReversalRepositoryTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcAccountRepository accountRepository;
    private JdbcCategoryRepository categoryRepository;
    private JdbcIncomeOperationRepository incomeOperationRepository;
    private JdbcExpenseOperationRepository expenseOperationRepository;
    private JdbcReversalRepository reversalRepository;

    private static final UUID USER_ID_A = UUID.randomUUID();
    private static final UUID USER_ID_B = UUID.randomUUID();
    private static final UUID ACCOUNT_ID_A = UUID.randomUUID();
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
        reversalRepository = new JdbcReversalRepository(connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            seedUser(conn, USER_ID_A);
            seedUser(conn, USER_ID_B);
        }

        transactionManager.execute(() -> {
            accountRepository.create(new Account(ACCOUNT_ID_A, USER_ID_A, "Account A"));
            categoryRepository.create(new Category(CATEGORY_INCOME_ID, USER_ID_A, "Salary", CategoryType.INCOME));
            categoryRepository.create(new Category(CATEGORY_EXPENSE_ID, USER_ID_A, "Rent", CategoryType.EXPENSE));
        });
    }

    @Test
    void createsAndFindsReversalByIdForIncome() {
        UUID incomeId = UUID.randomUUID();
        UUID reversalId = UUID.randomUUID();
        Instant now = Instant.now();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, Money.of("1000.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_INCOME_ID);
            incomeOperationRepository.create(income);
            income.cancel();
            incomeOperationRepository.update(income);

            Reversal reversal = new Reversal(reversalId, income, now);
            reversalRepository.create(reversal);
        });

        Optional<Reversal> found = transactionManager.execute(() -> reversalRepository.findById(reversalId));

        assertTrue(found.isPresent());
        assertEquals(reversalId, found.get().id());
        assertEquals(USER_ID_A, found.get().userId());
        assertEquals(incomeId, found.get().originalOperationId());
        assertEquals(Money.of("1000.00"), found.get().amount());
        assertEquals(now, found.get().cancelledAt());
    }

    @Test
    void createsAndFindsReversalByIdForExpense() {
        UUID expenseId = UUID.randomUUID();
        UUID reversalId = UUID.randomUUID();
        Instant now = Instant.now();

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_ID_A, Money.of("250.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_EXPENSE_ID);
            expenseOperationRepository.create(expense);
            expense.cancel();
            expenseOperationRepository.update(expense);

            Reversal reversal = new Reversal(reversalId, expense, now);
            reversalRepository.create(reversal);
        });

        Optional<Reversal> found = transactionManager.execute(() -> reversalRepository.findById(reversalId));

        assertTrue(found.isPresent());
        assertEquals(reversalId, found.get().id());
        assertEquals(USER_ID_A, found.get().userId());
        assertEquals(expenseId, found.get().originalOperationId());
        assertEquals(Money.of("250.00"), found.get().amount());
        assertEquals(now, found.get().cancelledAt());
    }

    @Test
    void findsReversalByOriginalOperationId() {
        UUID incomeId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        UUID incomeReversalId = UUID.randomUUID();
        UUID expenseReversalId = UUID.randomUUID();
        Instant now = Instant.now();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, Money.of("500.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_INCOME_ID);
            incomeOperationRepository.create(income);
            reversalRepository.create(new Reversal(incomeReversalId, income, now));

            Expense expense = new Expense(expenseId, USER_ID_A, Money.of("150.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_EXPENSE_ID);
            expenseOperationRepository.create(expense);
            reversalRepository.create(new Reversal(expenseReversalId, expense, now));
        });

        Optional<Reversal> foundIncomeReversal = transactionManager.execute(
            () -> reversalRepository.findByOriginalOperationId(incomeId));
        Optional<Reversal> foundExpenseReversal = transactionManager.execute(
            () -> reversalRepository.findByOriginalOperationId(expenseId));

        assertTrue(foundIncomeReversal.isPresent());
        assertEquals(incomeReversalId, foundIncomeReversal.get().id());

        assertTrue(foundExpenseReversal.isPresent());
        assertEquals(expenseReversalId, foundExpenseReversal.get().id());
    }

    @Test
    void returnsEmptyOptionalWhenReversalNotFound() {
        Optional<Reversal> byId = transactionManager.execute(
            () -> reversalRepository.findById(UUID.randomUUID()));
        Optional<Reversal> byOpId = transactionManager.execute(
            () -> reversalRepository.findByOriginalOperationId(UUID.randomUUID()));

        assertFalse(byId.isPresent());
        assertFalse(byOpId.isPresent());
    }

    @Test
    void violatesUniquenessConstraintOnDuplicateOriginalOperation() {
        UUID incomeId = UUID.randomUUID();
        Instant now = Instant.now();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, Money.of("500.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_INCOME_ID);
            incomeOperationRepository.create(income);
            reversalRepository.create(new Reversal(income, now));
        });

        assertThrows(RuntimeException.class, () -> transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, Money.of("500.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_INCOME_ID);
            reversalRepository.create(new Reversal(income, now));
        }));
    }

    @Test
    void throwsExceptionWhenOrphanedReversalExists() {
        UUID orphanedReversalId = UUID.randomUUID();
        UUID missingOperationId = UUID.randomUUID();
        Instant now = Instant.now();

        transactionManager.execute(() -> {
            Income income = new Income(USER_ID_A, Money.of("100.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_INCOME_ID);
            reversalRepository.create(new Reversal(orphanedReversalId, income, now));
        });

        transactionManager.execute(() -> {
            Connection conn = connectionHolder.get();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO reversals (id, original_operation_id, cancelled_at) VALUES ('"
                    + UUID.randomUUID() + "', '" + missingOperationId + "', '" + now + "')");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        RuntimeException ex1 = assertThrows(RuntimeException.class, () -> transactionManager.execute(
            () -> reversalRepository.findByOriginalOperationId(missingOperationId)));
        assertTrue(ex1.getMessage().contains("original operation not found"));
    }

    @Test
    void deleteByIdRemovesRecord() {
        UUID incomeId = UUID.randomUUID();
        UUID reversalId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, Money.of("500.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_INCOME_ID);
            incomeOperationRepository.create(income);
            reversalRepository.create(new Reversal(reversalId, income, Instant.now()));
        });

        transactionManager.execute(() -> reversalRepository.deleteById(reversalId));

        Optional<Reversal> found = transactionManager.execute(() -> reversalRepository.findById(reversalId));

        assertFalse(found.isPresent());
    }

    @Test
    void requiresActiveTransactionForRepositoryOperations() {
        assertThrows(IllegalStateException.class,
            () -> reversalRepository.findById(UUID.randomUUID()));
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
