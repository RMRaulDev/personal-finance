package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Income;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.OccurrenceResolution;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.domain.ResolutionStatus;
import com.rauldev.personalfinance.domain.User;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcUserRepositoryTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcUserRepository userRepository;
    private JdbcAccountRepository accountRepository;
    private JdbcCategoryRepository categoryRepository;
    private JdbcIncomeOperationRepository incomeOperationRepository;
    private JdbcExpenseOperationRepository expenseOperationRepository;
    private JdbcObligationRepository obligationRepository;
    private JdbcOccurrenceResolutionRepository resolutionRepository;

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        userRepository = new JdbcUserRepository(connectionHolder);
        accountRepository = new JdbcAccountRepository(connectionHolder);
        categoryRepository = new JdbcCategoryRepository(connectionHolder);
        incomeOperationRepository = new JdbcIncomeOperationRepository(connectionHolder);
        expenseOperationRepository = new JdbcExpenseOperationRepository(connectionHolder);
        obligationRepository = new JdbcObligationRepository(connectionHolder);
        resolutionRepository = new JdbcOccurrenceResolutionRepository(connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
        }
    }

    @Test
    void rejectsNullConnectionHolder() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new JdbcUserRepository(null));

        assertEquals("Connection holder cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullUserOnCreate() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> transactionManager.execute(() -> userRepository.create(null)));

        assertEquals("User cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullUserIdOnDelete() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> transactionManager.execute(() -> userRepository.deleteById(null)));

        assertEquals("User id cannot be null", e.getMessage());
    }

    @Test
    void requiresActiveTransactionToCreate() throws Exception {
        assertThrows(IllegalStateException.class, () -> userRepository.create(new User()));

        assertEquals(0, countUsers());
    }

    @Test
    void requiresActiveTransactionToDelete() throws Exception {
        User user = new User(UUID.randomUUID());
        transactionManager.execute(() -> userRepository.create(user));

        assertThrows(IllegalStateException.class, () -> userRepository.deleteById(user.id()));

        assertTrue(userExists(user.id()));
    }

    @Test
    void createsUserRowAndReturnsSameInstance() throws Exception {
        User user = new User(UUID.randomUUID());

        User created = transactionManager.execute(() -> userRepository.create(user));

        assertSame(user, created);
        assertTrue(userExists(user.id()));
        assertEquals(1, countUsers());
    }

    @Test
    void createdUserHasDefaultCreatedAt() throws Exception {
        User user = new User(UUID.randomUUID());

        transactionManager.execute(() -> userRepository.create(user));

        String createdAt = createdAt(user.id());

        assertDoesNotThrow(() -> Instant.parse(createdAt));
    }

    @Test
    void failsToCreateUserWithDuplicateId() throws Exception {
        User user = new User(UUID.randomUUID());
        transactionManager.execute(() -> userRepository.create(user));

        RuntimeException e = assertThrows(RuntimeException.class,
            () -> transactionManager.execute(() -> userRepository.create(new User(user.id()))));

        assertEquals("Failed to create user", e.getMessage());
        assertInstanceOf(SQLException.class, e.getCause());
        assertEquals(1, countUsers());
    }

    @Test
    void deletesExistingUser() throws Exception {
        User user = new User(UUID.randomUUID());
        User other = new User(UUID.randomUUID());
        transactionManager.execute(() -> {
            userRepository.create(user);
            userRepository.create(other);
        });

        transactionManager.execute(() -> userRepository.deleteById(user.id()));

        assertFalse(userExists(user.id()));
        assertTrue(userExists(other.id()));
    }

    @Test
    void deletingNonExistentUserCompletesWithoutError() throws Exception {
        User user = new User(UUID.randomUUID());
        transactionManager.execute(() -> userRepository.create(user));

        transactionManager.execute(() -> userRepository.deleteById(UUID.randomUUID()));

        assertTrue(userExists(user.id()));
        assertEquals(1, countUsers());
    }

    @Test
    void deletingUserCascadesToAccountsAndCategories() throws Exception {
        User user = new User(UUID.randomUUID());
        User other = new User(UUID.randomUUID());
        transactionManager.execute(() -> {
            userRepository.create(user);
            userRepository.create(other);
            accountRepository.create(new Account(user.id(), "Checking"));
            categoryRepository.create(new Category(user.id(), "Salary", CategoryType.INCOME));
            accountRepository.create(new Account(other.id(), "Checking"));
            categoryRepository.create(new Category(other.id(), "Salary", CategoryType.INCOME));
        });

        transactionManager.execute(() -> userRepository.deleteById(user.id()));

        assertFalse(userExists(user.id()));
        assertEquals(0, countByUser("accounts", user.id()));
        assertEquals(0, countByUser("categories", user.id()));
        assertEquals(1, countByUser("accounts", other.id()));
        assertEquals(1, countByUser("categories", other.id()));
    }

    @Test
    void failsToDeleteUserWhoseAccountIsReferencedByOperation() throws Exception {
        User user = new User(UUID.randomUUID());
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        transactionManager.execute(() -> {
            userRepository.create(user);
            accountRepository.create(new Account(accountId, user.id(), "Checking"));
            categoryRepository.create(new Category(categoryId, user.id(), "Salary", CategoryType.INCOME));
            incomeOperationRepository.create(new Income(UUID.randomUUID(), user.id(), Money.of("10.00"),
                LocalDate.of(2026, 8, 20), accountId, categoryId));
        });

        RuntimeException e = assertThrows(RuntimeException.class,
            () -> transactionManager.execute(() -> userRepository.deleteById(user.id())));

        assertEquals("Failed to delete user by id", e.getMessage());
        assertInstanceOf(SQLException.class, e.getCause());
        assertTrue(userExists(user.id()));
        assertEquals(1, countByUser("accounts", user.id()));
        assertEquals(1, countByUser("categories", user.id()));
    }

    @Test
    void deletingUserWithObligationAndSkippedResolutionAndNoOperationsDeletesEverything() throws Exception {
        User user = new User(UUID.randomUUID());
        User other = new User(UUID.randomUUID());
        UUID obligationId = UUID.randomUUID();
        UUID otherObligationId = UUID.randomUUID();
        transactionManager.execute(() -> {
            userRepository.create(user);
            userRepository.create(other);
            seedObligationWithSkippedResolution(user.id(), obligationId);
            seedObligationWithSkippedResolution(other.id(), otherObligationId);
        });

        transactionManager.execute(() -> userRepository.deleteById(user.id()));

        assertFalse(userExists(user.id()));
        assertEquals(0, countByUser("accounts", user.id()));
        assertEquals(0, countByUser("categories", user.id()));
        assertEquals(0, countByUser("obligations", user.id()));
        assertEquals(0, countWhere("occurrence_resolutions", "obligation_id = '" + obligationId + "'"));
        assertTrue(userExists(other.id()));
        assertEquals(1, countByUser("accounts", other.id()));
        assertEquals(1, countByUser("categories", other.id()));
        assertEquals(1, countByUser("obligations", other.id()));
        assertEquals(1, countWhere("occurrence_resolutions", "obligation_id = '" + otherObligationId + "'"));
    }

    @Test
    void failsToDeleteUserWithPaidResolutionAndRemovesNothing() throws Exception {
        User user = new User(UUID.randomUUID());
        UUID obligationId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        transactionManager.execute(() -> {
            userRepository.create(user);
            seedObligation(user.id(), obligationId);
            expenseOperationRepository.create(new Expense(expenseId, user.id(), Money.of("10.00"),
                LocalDate.of(2026, 9, 1), accountIdOf(obligationId), categoryIdOf(obligationId)));
            resolutionRepository.create(new OccurrenceResolution(UUID.randomUUID(), obligationId,
                LocalDate.of(2026, 9, 1), ResolutionStatus.PAID, expenseId, Instant.parse("2026-09-01T10:00:00Z")));
        });

        RuntimeException e = assertThrows(RuntimeException.class,
            () -> transactionManager.execute(() -> userRepository.deleteById(user.id())));

        assertEquals("Failed to delete user by id", e.getMessage());
        assertInstanceOf(SQLException.class, e.getCause());
        assertTrue(userExists(user.id()));
        assertEquals(1, countByUser("accounts", user.id()));
        assertEquals(1, countByUser("categories", user.id()));
        assertEquals(1, countByUser("obligations", user.id()));
        assertEquals(1, countWhere("occurrence_resolutions", "obligation_id = '" + obligationId + "'"));
        assertEquals(1, countWhere("expense_operations", "id = '" + expenseId + "'"));
    }

    @Test
    void directDeleteOfAccountReferencedByObligationFails() throws Exception {
        User user = new User(UUID.randomUUID());
        UUID obligationId = UUID.randomUUID();
        transactionManager.execute(() -> {
            userRepository.create(user);
            seedObligation(user.id(), obligationId);
        });

        assertThrows(SQLException.class,
            () -> executeRaw("DELETE FROM accounts WHERE id = '" + accountIdOf(obligationId) + "'"));

        assertEquals(1, countByUser("accounts", user.id()));
        assertEquals(1, countByUser("obligations", user.id()));
    }

    @Test
    void directDeleteOfCategoryReferencedByObligationFails() throws Exception {
        User user = new User(UUID.randomUUID());
        UUID obligationId = UUID.randomUUID();
        transactionManager.execute(() -> {
            userRepository.create(user);
            seedObligation(user.id(), obligationId);
        });

        assertThrows(SQLException.class,
            () -> executeRaw("DELETE FROM categories WHERE id = '" + categoryIdOf(obligationId) + "'"));

        assertEquals(1, countByUser("categories", user.id()));
        assertEquals(1, countByUser("obligations", user.id()));
    }

    @Test
    void directDeleteOfExpenseReferencedByResolutionFails() throws Exception {
        User user = new User(UUID.randomUUID());
        UUID obligationId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        transactionManager.execute(() -> {
            userRepository.create(user);
            seedObligation(user.id(), obligationId);
            expenseOperationRepository.create(new Expense(expenseId, user.id(), Money.of("10.00"),
                LocalDate.of(2026, 9, 1), accountIdOf(obligationId), categoryIdOf(obligationId)));
            resolutionRepository.create(new OccurrenceResolution(UUID.randomUUID(), obligationId,
                LocalDate.of(2026, 9, 1), ResolutionStatus.PAID, expenseId, Instant.parse("2026-09-01T10:00:00Z")));
        });

        assertThrows(SQLException.class,
            () -> executeRaw("DELETE FROM expense_operations WHERE id = '" + expenseId + "'"));

        assertEquals(1, countWhere("expense_operations", "id = '" + expenseId + "'"));
        assertEquals(1, countWhere("occurrence_resolutions", "obligation_id = '" + obligationId + "'"));
    }

    // Each obligation gets its own account and category, derived from the obligation id.
    private static UUID accountIdOf(UUID obligationId) {
        return UUID.nameUUIDFromBytes(("account-" + obligationId).getBytes(StandardCharsets.UTF_8));
    }

    private static UUID categoryIdOf(UUID obligationId) {
        return UUID.nameUUIDFromBytes(("category-" + obligationId).getBytes(StandardCharsets.UTF_8));
    }

    private void seedObligation(UUID userId, UUID obligationId) {
        accountRepository.create(new Account(accountIdOf(obligationId), userId, "Checking"));
        categoryRepository.create(new Category(categoryIdOf(obligationId), userId, "Rent", CategoryType.EXPENSE));
        obligationRepository.create(new Obligation(obligationId, userId, "Rent", Money.of("10.00"),
            accountIdOf(obligationId), categoryIdOf(obligationId),
            new Recurrence(Frequency.MONTHLY, LocalDate.of(2026, 8, 1), null), ObligationStatus.ACTIVE));
    }

    private void seedObligationWithSkippedResolution(UUID userId, UUID obligationId) {
        seedObligation(userId, obligationId);
        resolutionRepository.create(new OccurrenceResolution(UUID.randomUUID(), obligationId,
            LocalDate.of(2026, 8, 1), ResolutionStatus.SKIPPED, null, Instant.parse("2026-09-01T10:00:00Z")));
    }

    private void executeRaw(String sql) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private int countWhere(String table, String condition) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + condition)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private boolean userExists(UUID userId) throws SQLException {
        return countByUser("users", userId, "id") == 1;
    }

    private int countUsers() throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countByUser(String table, UUID userId) throws SQLException {
        return countByUser(table, userId, "user_id");
    }

    private int countByUser(String table, UUID userId, String column) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = '" + userId + "'")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private String createdAt(UUID userId) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT created_at FROM users WHERE id = '" + userId + "'")) {
            assertTrue(rs.next());
            return rs.getString(1);
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
