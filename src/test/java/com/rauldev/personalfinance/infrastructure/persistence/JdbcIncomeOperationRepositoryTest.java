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
import com.rauldev.personalfinance.domain.Income;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcIncomeOperationRepositoryTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcAccountRepository accountRepository;
    private JdbcCategoryRepository categoryRepository;
    private JdbcIncomeOperationRepository incomeOperationRepository;

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
        incomeOperationRepository = new JdbcIncomeOperationRepository(connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            seedUser(conn, USER_ID_A);
            seedUser(conn, USER_ID_B);
        }

        transactionManager.execute(() -> {
            accountRepository.create(new Account(ACCOUNT_ID_A, USER_ID_A, "Account A"));
            accountRepository.create(new Account(ACCOUNT_ID_B, USER_ID_B, "Account B"));
            categoryRepository.create(new Category(CATEGORY_ID_A, USER_ID_A, "Salary", CategoryType.INCOME));
            categoryRepository.create(new Category(CATEGORY_ID_B, USER_ID_B, "Freelance", CategoryType.INCOME));
        });
    }

    @Test
    void createsAndFindsIncomeById() {
        UUID incomeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 2);
        Money amount = Money.of("1500.50");

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, amount, date, ACCOUNT_ID_A, CATEGORY_ID_A);
            incomeOperationRepository.create(income);
        });

        Optional<Income> found = transactionManager.execute(() -> incomeOperationRepository.findById(incomeId));

        assertTrue(found.isPresent());
        assertEquals(incomeId, found.get().id());
        assertEquals(USER_ID_A, found.get().userId());
        assertEquals(ACCOUNT_ID_A, found.get().accountId());
        assertEquals(CATEGORY_ID_A, found.get().categoryId());
        assertEquals(amount, found.get().amount());
        assertEquals(date, found.get().operationDate());
        assertEquals(OperationStatus.ACTIVE, found.get().status());
    }

    @Test
    void findsIncomeByIdAndCorrectUserId() {
        UUID incomeId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, Money.of("100.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            incomeOperationRepository.create(income);
        });

        Optional<Income> found = transactionManager.execute(
            () -> incomeOperationRepository.findByIdAndUserId(incomeId, USER_ID_A));

        assertTrue(found.isPresent());
        assertEquals(incomeId, found.get().id());
    }

    @Test
    void doesNotFindIncomeWhenUserIdDoesNotMatch() {
        UUID incomeId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, Money.of("100.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            incomeOperationRepository.create(income);
        });

        Optional<Income> found = transactionManager.execute(
            () -> incomeOperationRepository.findByIdAndUserId(incomeId, USER_ID_B));

        assertFalse(found.isPresent());
    }

    @Test
    void findsAllIncomeOperationsForUser() {
        transactionManager.execute(() -> {
            incomeOperationRepository.create(
                new Income(USER_ID_A, Money.of("100.00"), LocalDate.of(2026, 9, 1), ACCOUNT_ID_A, CATEGORY_ID_A));
            incomeOperationRepository.create(
                new Income(USER_ID_A, Money.of("200.00"), LocalDate.of(2026, 9, 2), ACCOUNT_ID_A, CATEGORY_ID_A));
            incomeOperationRepository.create(
                new Income(USER_ID_B, Money.of("300.00"), LocalDate.of(2026, 9, 2), ACCOUNT_ID_B, CATEGORY_ID_B));
        });

        List<Income> userAIncomes = transactionManager.execute(() -> incomeOperationRepository.findByUserId(USER_ID_A));
        List<Income> userBIncomes = transactionManager.execute(() -> incomeOperationRepository.findByUserId(USER_ID_B));

        assertEquals(2, userAIncomes.size());
        assertEquals(1, userBIncomes.size());
        assertEquals(Money.of("200.00"), userAIncomes.get(0).amount());
        assertEquals(Money.of("100.00"), userAIncomes.get(1).amount());
    }

    @Test
    void reconstructsPersistedFieldsCorrectly() {
        UUID incomeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 20);
        Money amount = Money.of("750.25");

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, amount, date, ACCOUNT_ID_A, CATEGORY_ID_A);
            incomeOperationRepository.create(income);
        });

        Income found = transactionManager.execute(() -> incomeOperationRepository.findById(incomeId).orElseThrow());

        assertEquals(incomeId, found.id());
        assertEquals(USER_ID_A, found.userId());
        assertEquals(ACCOUNT_ID_A, found.accountId());
        assertEquals(CATEGORY_ID_A, found.categoryId());
        assertEquals(amount, found.amount());
        assertEquals(date, found.operationDate());
    }

    @Test
    void reconstructsActiveStatusCorrectly() {
        UUID incomeId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, Money.of("500.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            incomeOperationRepository.create(income);
        });

        Income found = transactionManager.execute(() -> incomeOperationRepository.findById(incomeId).orElseThrow());

        assertEquals(OperationStatus.ACTIVE, found.status());
    }

    @Test
    void reconstructsCancelledStatusCorrectly() {
        UUID incomeId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, Money.of("500.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            income.cancel();
            incomeOperationRepository.create(income);
        });

        Income found = transactionManager.execute(() -> incomeOperationRepository.findById(incomeId).orElseThrow());

        assertEquals(OperationStatus.CANCELLED, found.status());
    }

    @Test
    void updatePersistsCancelledStatus() {
        UUID incomeId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, Money.of("500.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            incomeOperationRepository.create(income);
        });

        transactionManager.execute(() -> {
            Income income = incomeOperationRepository.findById(incomeId).orElseThrow();
            income.cancel();
            incomeOperationRepository.update(income);
        });

        Income updated = transactionManager.execute(() -> incomeOperationRepository.findById(incomeId).orElseThrow());

        assertEquals(OperationStatus.CANCELLED, updated.status());
    }

    @Test
    void deleteByIdRemovesRecord() {
        UUID incomeId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Income income = new Income(incomeId, USER_ID_A, Money.of("500.00"), LocalDate.now(), ACCOUNT_ID_A, CATEGORY_ID_A);
            incomeOperationRepository.create(income);
        });

        transactionManager.execute(() -> incomeOperationRepository.deleteById(incomeId));

        Optional<Income> found = transactionManager.execute(() -> incomeOperationRepository.findById(incomeId));

        assertFalse(found.isPresent());
    }

    @Test
    void violatesForeignKeyConstraintOnInvalidAccount() {
        UUID invalidAccountId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> transactionManager.execute(() -> {
            Income income = new Income(USER_ID_A, Money.of("500.00"), LocalDate.now(), invalidAccountId, CATEGORY_ID_A);
            incomeOperationRepository.create(income);
        }));
    }

    @Test
    void violatesForeignKeyConstraintOnInvalidCategory() {
        UUID invalidCategoryId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> transactionManager.execute(() -> {
            Income income = new Income(USER_ID_A, Money.of("500.00"), LocalDate.now(), ACCOUNT_ID_A, invalidCategoryId);
            incomeOperationRepository.create(income);
        }));
    }

    @Test
    void requiresActiveTransactionForRepositoryOperations() {
        assertThrows(IllegalStateException.class,
            () -> incomeOperationRepository.findById(UUID.randomUUID()));
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
