package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rauldev.personalfinance.application.readmodel.CategoryDetails;
import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcCategoryQueryAdapterTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcCategoryQueryAdapter categoryQueryAdapter;

    private static final UUID USER_A_ID = UUID.randomUUID();
    private static final UUID USER_B_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        categoryQueryAdapter = new JdbcCategoryQueryAdapter(connectionProvider, connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            seedUser(conn, USER_A_ID);
            seedUser(conn, USER_B_ID);
        }
    }

    @Test
    void findByUserId_returnsCategoriesOrderedByNameThenId() {
        insertCategoryRow(UUID.randomUUID(), USER_A_ID, "Transport", "EXPENSE", "ACTIVE");
        insertCategoryRow(UUID.randomUUID(), USER_A_ID, "Food", "EXPENSE", "ACTIVE");
        insertCategoryRow(UUID.randomUUID(), USER_A_ID, "Salary", "INCOME", "ACTIVE");

        List<CategoryDetails> categories = categoryQueryAdapter.findByUserId(USER_A_ID);

        assertEquals(List.of("Food", "Salary", "Transport"),
            categories.stream().map(CategoryDetails::name).toList());
    }

    @Test
    void findByUserId_ordersByNameWithBinaryCollationUppercaseBeforeLowercase() {
        insertCategoryRow(UUID.randomUUID(), USER_A_ID, "alpha", "EXPENSE", "ACTIVE");
        insertCategoryRow(UUID.randomUUID(), USER_A_ID, "Zeta", "EXPENSE", "ACTIVE");

        List<CategoryDetails> categories = categoryQueryAdapter.findByUserId(USER_A_ID);

        assertEquals(List.of("Zeta", "alpha"), categories.stream().map(CategoryDetails::name).toList());
    }

    @Test
    void findByUserId_mapsEveryFieldIncludingTypeAndStatus() {
        UUID incomeId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        insertCategoryRow(incomeId, USER_A_ID, "Salary", "INCOME", "ACTIVE");
        insertCategoryRow(expenseId, USER_A_ID, "Food", "EXPENSE", "INACTIVE");

        List<CategoryDetails> categories = categoryQueryAdapter.findByUserId(USER_A_ID);

        assertEquals(List.of(
            new CategoryDetails(expenseId, "Food", CategoryType.EXPENSE, CategoryStatus.INACTIVE),
            new CategoryDetails(incomeId, "Salary", CategoryType.INCOME, CategoryStatus.ACTIVE)),
            categories);
    }

    @Test
    void findByUserId_excludesOtherUsersCategories() {
        UUID ownId = UUID.randomUUID();
        insertCategoryRow(ownId, USER_A_ID, "Mine", "EXPENSE", "ACTIVE");
        insertCategoryRow(UUID.randomUUID(), USER_B_ID, "Theirs", "EXPENSE", "ACTIVE");

        List<CategoryDetails> categories = categoryQueryAdapter.findByUserId(USER_A_ID);

        assertEquals(1, categories.size());
        assertEquals(ownId, categories.get(0).id());
    }

    @Test
    void findByUserId_returnsEmptyListWhenUserHasNoCategories() {
        insertCategoryRow(UUID.randomUUID(), USER_B_ID, "Theirs", "EXPENSE", "ACTIVE");

        assertEquals(List.of(), categoryQueryAdapter.findByUserId(USER_A_ID));
    }

    @Test
    void findByUserId_worksOutsideAndInsideActiveTransaction() {
        insertCategoryRow(UUID.randomUUID(), USER_A_ID, "Food", "EXPENSE", "ACTIVE");

        List<CategoryDetails> outside = categoryQueryAdapter.findByUserId(USER_A_ID);
        List<CategoryDetails> inside = transactionManager.execute(() -> categoryQueryAdapter.findByUserId(USER_A_ID));

        assertEquals(1, outside.size());
        assertEquals(outside, inside);
    }

    @Test
    void findByUserId_insideTransactionReusesBoundConnectionWithoutClosingIt() {
        UUID uncommitted = UUID.randomUUID();

        transactionManager.execute(() -> {
            Connection bound = connectionHolder.get();
            try (Statement stmt = bound.createStatement()) {
                stmt.executeUpdate("INSERT INTO categories (id, user_id, name, type, status) VALUES ('"
                    + uncommitted + "', '" + USER_A_ID + "', 'Pending', 'EXPENSE', 'ACTIVE')");

                List<CategoryDetails> categories = categoryQueryAdapter.findByUserId(USER_A_ID);

                assertEquals(1, categories.size());
                assertEquals(uncommitted, categories.get(0).id());
                assertFalse(bound.isClosed());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void findByUserId_rejectsNullUserId() {
        assertThrows(NullPointerException.class, () -> categoryQueryAdapter.findByUserId(null));
    }

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenTypeIsInvalid() {
        UUID rowId = UUID.randomUUID();
        insertCategoryRow(rowId, USER_A_ID, "Broken", "BOGUS", "ACTIVE");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> categoryQueryAdapter.findByUserId(USER_A_ID));

        assertTrue(ex.getMessage().contains("categories"));
        assertTrue(ex.getMessage().contains(rowId.toString()));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenStatusIsInvalid() {
        UUID rowId = UUID.randomUUID();
        insertCategoryRow(rowId, USER_A_ID, "Broken", "EXPENSE", "BOGUS");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> categoryQueryAdapter.findByUserId(USER_A_ID));

        assertTrue(ex.getMessage().contains("categories"));
        assertTrue(ex.getMessage().contains(rowId.toString()));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenNameIsBlank() {
        UUID rowId = UUID.randomUUID();
        insertCategoryRow(rowId, USER_A_ID, "  ", "EXPENSE", "ACTIVE");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> categoryQueryAdapter.findByUserId(USER_A_ID));

        assertTrue(ex.getMessage().contains("categories"));
        assertTrue(ex.getMessage().contains(rowId.toString()));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    private void insertCategoryRow(UUID id, UUID userId, String name, String type, String status) {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO categories (id, user_id, name, type, status) VALUES ('" + id + "', '"
                + userId + "', '" + name + "', '" + type + "', '" + status + "')");
        } catch (SQLException e) {
            throw new RuntimeException(e);
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

    private void seedUser(Connection connection, UUID userId) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO users (id) VALUES ('" + userId + "')");
        }
    }
}
