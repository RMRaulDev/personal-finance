package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcCategoryRepositoryTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcCategoryRepository categoryRepository;

    private static final UUID USER_ID_A = UUID.randomUUID();
    private static final UUID USER_ID_B = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        categoryRepository = new JdbcCategoryRepository(connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            seedUser(conn, USER_ID_A);
            seedUser(conn, USER_ID_B);
        }
    }

    @Test
    void createsAndFindsCategoryById() {
        UUID categoryId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Category category = new Category(categoryId, USER_ID_A, "Salary", CategoryType.INCOME);
            categoryRepository.create(category);
        });

        Optional<Category> found = transactionManager.execute(() -> categoryRepository.findById(categoryId));

        assertTrue(found.isPresent());
        assertEquals(categoryId, found.get().id());
        assertEquals(USER_ID_A, found.get().userId());
        assertEquals("Salary", found.get().name());
        assertEquals(CategoryType.INCOME, found.get().type());
        assertEquals(CategoryStatus.ACTIVE, found.get().status());
    }

    @Test
    void findsCategoryByIdAndCorrectUserId() {
        UUID categoryId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Category category = new Category(categoryId, USER_ID_A, "Groceries", CategoryType.EXPENSE);
            categoryRepository.create(category);
        });

        Optional<Category> found = transactionManager.execute(
            () -> categoryRepository.findByIdAndUserId(categoryId, USER_ID_A));

        assertTrue(found.isPresent());
        assertEquals(categoryId, found.get().id());
    }

    @Test
    void doesNotFindCategoryWhenUserIdDoesNotMatch() {
        UUID categoryId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Category category = new Category(categoryId, USER_ID_A, "Groceries", CategoryType.EXPENSE);
            categoryRepository.create(category);
        });

        Optional<Category> found = transactionManager.execute(
            () -> categoryRepository.findByIdAndUserId(categoryId, USER_ID_B));

        assertFalse(found.isPresent());
    }

    @Test
    void findsAllCategoriesForUser() {
        transactionManager.execute(() -> {
            categoryRepository.create(new Category(USER_ID_A, "Salary", CategoryType.INCOME));
            categoryRepository.create(new Category(USER_ID_A, "Groceries", CategoryType.EXPENSE));
            categoryRepository.create(new Category(USER_ID_B, "Rent", CategoryType.EXPENSE));
        });

        List<Category> userACategories = transactionManager.execute(() -> categoryRepository.findByUserId(USER_ID_A));
        List<Category> userBCategories = transactionManager.execute(() -> categoryRepository.findByUserId(USER_ID_B));

        assertEquals(2, userACategories.size());
        assertEquals(1, userBCategories.size());
    }

    @Test
    void checksExistenceByUserIdAndName() {
        transactionManager.execute(() -> {
            categoryRepository.create(new Category(USER_ID_A, "Salary", CategoryType.INCOME));
        });

        boolean existsForUserA = transactionManager.execute(
            () -> categoryRepository.existsByUserIdAndName(USER_ID_A, "Salary"));
        boolean doesNotExistForUserA = transactionManager.execute(
            () -> categoryRepository.existsByUserIdAndName(USER_ID_A, "Unknown"));
        boolean doesNotExistForUserB = transactionManager.execute(
            () -> categoryRepository.existsByUserIdAndName(USER_ID_B, "Salary"));

        assertTrue(existsForUserA);
        assertFalse(doesNotExistForUserA);
        assertFalse(doesNotExistForUserB);
    }

    @Test
    void updatesNameAndStatus() {
        UUID categoryId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Category category = new Category(categoryId, USER_ID_A, "Salary", CategoryType.INCOME);
            categoryRepository.create(category);
        });

        transactionManager.execute(() -> {
            Category category = categoryRepository.findById(categoryId).orElseThrow();
            category.rename("Primary Income");
            category.deactivate();
            categoryRepository.update(category);
        });

        Optional<Category> updated = transactionManager.execute(() -> categoryRepository.findById(categoryId));

        assertTrue(updated.isPresent());
        assertEquals(categoryId, updated.get().id());
        assertEquals(USER_ID_A, updated.get().userId());
        assertEquals("Primary Income", updated.get().name());
        assertEquals(CategoryType.INCOME, updated.get().type());
        assertEquals(CategoryStatus.INACTIVE, updated.get().status());
    }

    @Test
    void violatesUniquenessConstraintForUserIdAndName() {
        transactionManager.execute(() -> {
            categoryRepository.create(new Category(USER_ID_A, "Salary", CategoryType.INCOME));
        });

        assertThrows(RuntimeException.class, () -> transactionManager.execute(
            () -> categoryRepository.create(new Category(USER_ID_A, "Salary", CategoryType.INCOME))));
    }

    @Test
    void requiresActiveTransactionForRepositoryOperations() {
        assertThrows(IllegalStateException.class,
            () -> categoryRepository.findById(UUID.randomUUID()));
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
