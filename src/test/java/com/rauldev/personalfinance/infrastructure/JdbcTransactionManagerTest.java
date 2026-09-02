package com.rauldev.personalfinance.infrastructure;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rauldev.personalfinance.infrastructure.persistence.SQLiteConnectionProvider;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcTransactionManagerTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
        }
    }

    @AfterEach
    void tearDown() {
        if (connectionHolder.hasActiveTransaction()) {
            connectionHolder.clear();
        }
    }

    @Test
    void test1_successfulTransactionCommits() throws SQLException {
        UUID userId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Connection conn = connectionHolder.get();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO users (id) VALUES ('" + userId + "')");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        assertFalse(connectionHolder.hasActiveTransaction());

        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE id = '" + userId + "'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void test2_failedTransactionRollsBack() throws SQLException {
        UUID userId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> {
            transactionManager.execute(() -> {
                Connection conn = connectionHolder.get();
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("INSERT INTO users (id) VALUES ('" + userId + "')");
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                throw new RuntimeException("Simulated business error");
            });
        });

        assertFalse(connectionHolder.hasActiveTransaction());

        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE id = '" + userId + "'")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    void test3_sameConnectionIsReused() {
        transactionManager.execute(() -> {
            Connection first = connectionHolder.get();
            Connection second = connectionHolder.get();

            assertNotNull(first);
            assertSame(first, second);
        });
    }

    @Test
    void test4_nestedTransactionIsRejected() {
        transactionManager.execute(() -> {
            Connection outerConnection = connectionHolder.get();

            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> transactionManager.execute(() -> null)
            );

            assertTrue(ex.getMessage().contains("Nested transactions are not supported"));
            assertSame(outerConnection, connectionHolder.get());
        });

        assertFalse(connectionHolder.hasActiveTransaction());
    }

    @Test
    void test5_cleanupAfterFailure() {
        assertThrows(RuntimeException.class, () -> {
            transactionManager.execute(() -> {
                throw new RuntimeException("Failure");
            });
        });

        assertFalse(connectionHolder.hasActiveTransaction());

        UUID userId = UUID.randomUUID();
        transactionManager.execute(() -> {
            Connection conn = connectionHolder.get();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO users (id) VALUES ('" + userId + "')");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        assertFalse(connectionHolder.hasActiveTransaction());
    }

    @Test
    void test6_independentTransactions() throws SQLException {
        UUID firstUser = UUID.randomUUID();
        UUID secondUser = UUID.randomUUID();

        transactionManager.execute(() -> {
            Connection conn = connectionHolder.get();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO users (id) VALUES ('" + firstUser + "')");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        transactionManager.execute(() -> {
            Connection conn = connectionHolder.get();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO users (id) VALUES ('" + secondUser + "')");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    void test7_foreignKeyEnforcement() {
        UUID nonExistentUserId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> {
            transactionManager.execute(() -> {
                Connection conn = connectionHolder.get();
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                        "INSERT INTO accounts (id, user_id, name, balance, status) VALUES ('"
                        + accountId + "', '" + nonExistentUserId + "', 'Checking', 100, 'ACTIVE')"
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    @Test
    void test8_repositoryAccessWithoutTransactionFails() {
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> connectionHolder.get()
        );

        assertTrue(ex.getMessage().contains("No active transaction found"));
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
