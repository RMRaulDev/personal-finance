package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
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
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcExpenseOperationRepositoryTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcAccountRepository accountRepository;
    private JdbcCategoryRepository categoryRepository;
    private JdbcExpenseOperationRepository expenseOperationRepository;

    private static final UUID USER_ID_A = UUID.randomUUID();
    private static final UUID USER_ID_B = UUID.randomUUID();
    private static final UUID ACCOUNT_ID_A = UUID.randomUUID();
    private static final UUID ACCOUNT_ID_B = UUID.randomUUID();
    private static final UUID CATEGORY_ID_A = UUID.randomUUID();
    private static final UUID CATEGORY_ID_B = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        accountRepository = new JdbcAccountRepository(connectionHolder);
        categoryRepository = new JdbcCategoryRepository(connectionHolder);
        expenseOperationRepository = new JdbcExpenseOperationRepository(connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            seedUser(conn, USER_ID_A);
            seedUser(conn, USER_ID_B);
        }

        transactionManager.execute(() -> {
            accountRepository.create(new Account(ACCOUNT_ID_A, USER_ID_A, "Account A"));
            accountRepository.create(new Account(ACCOUNT_ID_B, USER_ID_B, "Account B"));
            categoryRepository.create(new Category(CATEGORY_ID_A, USER_ID_A, "Groceries", CategoryType.EXPENSE));
            categoryRepository.create(new Category(CATEGORY_ID_B, USER_ID_B, "Rent", CategoryType.EXPENSE));
        });
    }

    @Test
    void createsAndFindsExpenseById() {
        UUID expenseId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 2);
        Money amount = Money.of("450.75");

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_ID_A, amount, date, ACCOUNT_ID_A, CATEGORY_ID_A);
            expenseOperationRepository.create(expense);
        });

        Optional<Expense> found = transactionManager.execute(() -> expenseOperationRepository.findById(expenseId));

        assertTrue(found.isPresent());
        assertEquals(expenseId, found.get().id());
        assertEquals(USER_ID_A, found.get().userId());
        assertEquals(ACCOUNT_ID_A, found.get().accountId());
        assertEquals(CATEGORY_ID_A, found.get().categoryId());
        assertEquals(amount, found.get().amount());
        assertEquals(date, found.get().operationDate());
        assertEquals(OperationStatus.ACTIVE, found.get().status());
    }

    @Test
    void findsExpenseByIdAndCorrectUserId() {
        UUID expenseId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_ID_A, Money.of("50.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            expenseOperationRepository.create(expense);
        });

        Optional<Expense> found = transactionManager.execute(
            () -> expenseOperationRepository.findByIdAndUserId(expenseId, USER_ID_A));

        assertTrue(found.isPresent());
        assertEquals(expenseId, found.get().id());
    }

    @Test
    void doesNotFindExpenseWhenUserIdDoesNotMatch() {
        UUID expenseId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_ID_A, Money.of("50.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            expenseOperationRepository.create(expense);
        });

        Optional<Expense> found = transactionManager.execute(
            () -> expenseOperationRepository.findByIdAndUserId(expenseId, USER_ID_B));

        assertFalse(found.isPresent());
    }

    @Test
    void findsAllExpenseOperationsForUser() {
        transactionManager.execute(() -> {
            expenseOperationRepository.create(
                new Expense(USER_ID_A, Money.of("100.00"), LocalDate.of(2026, 9, 1), ACCOUNT_ID_A, CATEGORY_ID_A));
            expenseOperationRepository.create(
                new Expense(USER_ID_A, Money.of("200.00"), LocalDate.of(2026, 9, 2), ACCOUNT_ID_A, CATEGORY_ID_A));
            expenseOperationRepository.create(
                new Expense(USER_ID_B, Money.of("300.00"), LocalDate.of(2026, 9, 2), ACCOUNT_ID_B, CATEGORY_ID_B));
        });

        List<Expense> userAExpenses = transactionManager.execute(() -> expenseOperationRepository.findByUserId(USER_ID_A));
        List<Expense> userBExpenses = transactionManager.execute(() -> expenseOperationRepository.findByUserId(USER_ID_B));

        assertEquals(2, userAExpenses.size());
        assertEquals(1, userBExpenses.size());
        assertEquals(Money.of("200.00"), userAExpenses.get(0).amount());
        assertEquals(Money.of("100.00"), userAExpenses.get(1).amount());
    }

    @Test
    void reconstructsPersistedFieldsCorrectly() {
        UUID expenseId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 15);
        Money amount = Money.of("120.50");

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_ID_A, amount, date, ACCOUNT_ID_A, CATEGORY_ID_A);
            expenseOperationRepository.create(expense);
        });

        Expense found = transactionManager.execute(() -> expenseOperationRepository.findById(expenseId).orElseThrow());

        assertEquals(expenseId, found.id());
        assertEquals(USER_ID_A, found.userId());
        assertEquals(ACCOUNT_ID_A, found.accountId());
        assertEquals(CATEGORY_ID_A, found.categoryId());
        assertEquals(amount, found.amount());
        assertEquals(date, found.operationDate());
    }

    @Test
    void reconstructsActiveStatusCorrectly() {
        UUID expenseId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_ID_A, Money.of("80.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            expenseOperationRepository.create(expense);
        });

        Expense found = transactionManager.execute(() -> expenseOperationRepository.findById(expenseId).orElseThrow());

        assertEquals(OperationStatus.ACTIVE, found.status());
    }

    @Test
    void reconstructsCancelledStatusCorrectly() {
        UUID expenseId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_ID_A, Money.of("80.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            expense.cancel();
            expenseOperationRepository.create(expense);
        });

        Expense found = transactionManager.execute(() -> expenseOperationRepository.findById(expenseId).orElseThrow());

        assertEquals(OperationStatus.CANCELLED, found.status());
    }

    @Test
    void updatePersistsCancelledStatus() {
        UUID expenseId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_ID_A, Money.of("80.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            expenseOperationRepository.create(expense);
        });

        transactionManager.execute(() -> {
            Expense expense = expenseOperationRepository.findById(expenseId).orElseThrow();
            expense.cancel();
            expenseOperationRepository.update(expense);
        });

        Expense updated = transactionManager.execute(() -> expenseOperationRepository.findById(expenseId).orElseThrow());

        assertEquals(OperationStatus.CANCELLED, updated.status());
    }

    @Test
    void deleteByIdRemovesRecord() {
        UUID expenseId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Expense expense = new Expense(expenseId, USER_ID_A, Money.of("80.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            expenseOperationRepository.create(expense);
        });

        transactionManager.execute(() -> expenseOperationRepository.deleteById(expenseId));

        Optional<Expense> found = transactionManager.execute(() -> expenseOperationRepository.findById(expenseId));

        assertFalse(found.isPresent());
    }

    @Test
    void violatesForeignKeyConstraintOnInvalidAccount() {
        UUID invalidAccountId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> transactionManager.execute(() -> {
            Expense expense = new Expense(USER_ID_A, Money.of("80.00"), LocalDate.now(), invalidAccountId, CATEGORY_ID_A);
            expenseOperationRepository.create(expense);
        }));
    }

    @Test
    void violatesForeignKeyConstraintOnInvalidCategory() {
        UUID invalidCategoryId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> transactionManager.execute(() -> {
            Expense expense = new Expense(USER_ID_A, Money.of("80.00"), LocalDate.now(), ACCOUNT_ID_A, invalidCategoryId);
            expenseOperationRepository.create(expense);
        }));
    }

    @Test
    void requiresActiveTransactionForRepositoryOperations() {
        assertThrows(IllegalStateException.class,
            () -> expenseOperationRepository.findById(UUID.randomUUID()));
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
