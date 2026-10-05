package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcUserQueryAdapterTest {

    private static final UUID EXISTING_USER = UUID.randomUUID();

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcUserQueryAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        connectionProvider = new SQLiteConnectionProvider("jdbc:sqlite:" + tempDir.resolve("test-finance.db").toAbsolutePath());
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        adapter = new JdbcUserQueryAdapter(connectionProvider, connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO users (id) VALUES ('" + EXISTING_USER + "')");
            }
        }
    }

    @Test
    void existsByIdReturnsTrueWhenUserExists() {
        assertTrue(adapter.existsById(EXISTING_USER));
    }

    @Test
    void existsByIdReturnsFalseWhenUserDoesNotExist() {
        assertFalse(adapter.existsById(UUID.randomUUID()));
    }

    @Test
    void existsByIdWorksInsideAnActiveTransactionWithoutClosingTheBoundConnection() {
        transactionManager.execute(() -> {
            Connection bound = connectionHolder.get();

            assertTrue(adapter.existsById(EXISTING_USER));
            assertFalse(adapter.existsById(UUID.randomUUID()));

            try {
                assertFalse(bound.isClosed());
            } catch (java.sql.SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void existsByIdSeesUncommittedRowsOfTheActiveTransaction() {
        UUID uncommitted = UUID.randomUUID();

        transactionManager.execute(() -> {
            try (Statement stmt = connectionHolder.get().createStatement()) {
                stmt.executeUpdate("INSERT INTO users (id) VALUES ('" + uncommitted + "')");
            } catch (java.sql.SQLException e) {
                throw new RuntimeException(e);
            }

            assertTrue(adapter.existsById(uncommitted));
        });
    }

    @Test
    void existsByIdRejectsNullUserId() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> adapter.existsById(null));

        assertEquals("User id cannot be null", ex.getMessage());
    }

    @Test
    void constructorRejectsNullDependencies() {
        assertThrows(NullPointerException.class, () -> new JdbcUserQueryAdapter(null, connectionHolder));
        assertThrows(NullPointerException.class, () -> new JdbcUserQueryAdapter(connectionProvider, null));
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
