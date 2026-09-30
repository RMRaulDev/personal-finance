package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteConstraintViolationsTest {
    private static final String ACCOUNT_NAME_COLUMNS = "accounts.user_id, accounts.name";
    private static final String CATEGORY_NAME_COLUMNS = "categories.user_id, categories.name";

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        connectionProvider = new SQLiteConnectionProvider("jdbc:sqlite:" + tempDir.resolve("test.db").toAbsolutePath());
        try (Connection conn = connectionProvider.getConnection()) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/schema.sql")) {
                String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(trimmed);
                        }
                    }
                }
            }
            execute(conn, "INSERT INTO users (id) VALUES ('" + userId + "')");
        }
    }

    @Test
    void detectsUniqueViolationOnMatchingColumns() throws SQLException {
        SQLException exception = failWith(accountInsert(UUID.randomUUID(), userId, "Same"),
            accountInsert(UUID.randomUUID(), userId, "Same"));

        assertTrue(SqliteConstraintViolations.isUniqueViolation(exception, ACCOUNT_NAME_COLUMNS));
    }

    @Test
    void rejectsPrimaryKeyViolation() throws SQLException {
        UUID id = UUID.randomUUID();

        SQLException exception = failWith(accountInsert(id, userId, "One"), accountInsert(id, userId, "Two"));

        assertFalse(SqliteConstraintViolations.isUniqueViolation(exception, ACCOUNT_NAME_COLUMNS));
    }

    @Test
    void rejectsForeignKeyViolation() throws SQLException {
        SQLException exception = failWith(null, accountInsert(UUID.randomUUID(), UUID.randomUUID(), "Orphan"));

        assertFalse(SqliteConstraintViolations.isUniqueViolation(exception, ACCOUNT_NAME_COLUMNS));
    }

    @Test
    void rejectsUniqueViolationWhenColumnsDoNotMatch() throws SQLException {
        SQLException exception = failWith(accountInsert(UUID.randomUUID(), userId, "Same"),
            accountInsert(UUID.randomUUID(), userId, "Same"));

        assertFalse(SqliteConstraintViolations.isUniqueViolation(exception, CATEGORY_NAME_COLUMNS));
    }

    @Test
    void rejectsNonSqliteException() {
        assertFalse(SqliteConstraintViolations.isUniqueViolation(
            new SQLException("UNIQUE constraint failed: " + ACCOUNT_NAME_COLUMNS), ACCOUNT_NAME_COLUMNS));
    }

    private static String accountInsert(UUID id, UUID user, String name) {
        return "INSERT INTO accounts (id, user_id, name, balance, status) VALUES ('" + id + "', '" + user
            + "', '" + name + "', 0, 'ACTIVE')";
    }

    private SQLException failWith(String setupSql, String failingSql) throws SQLException {
        try (Connection conn = connectionProvider.getConnection()) {
            execute(conn, "PRAGMA foreign_keys = ON");
            if (setupSql != null) {
                execute(conn, setupSql);
            }
            return assertThrows(SQLException.class, () -> execute(conn, failingSql));
        }
    }

    private static void execute(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
