package com.rauldev.personalfinance.entry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

/**
 * Real SQLite file for entry tests. The schema comes from {@code db/schema.sql}; rows are seeded
 * and read back with plain SQL, independently of the production adapters.
 */
public final class SqliteTestDatabase {
    private static final List<String> TABLES_IN_DELETE_ORDER = List.of(
        "occurrence_resolutions", "reversals", "income_operations", "expense_operations",
        "transfer_operations", "obligations", "accounts", "categories", "users");

    private final String url;

    public SqliteTestDatabase(Path file) {
        this.url = "jdbc:sqlite:" + file.toAbsolutePath();
        try (Connection connection = open(); InputStream is = schema()) {
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(trimmed);
                    }
                }
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Cannot initialize test database", e);
        }
    }

    public String url() {
        return url;
    }

    public void reset() {
        for (String table : TABLES_IN_DELETE_ORDER) {
            execute("DELETE FROM " + table);
        }
    }

    public void insertUser(UUID userId) {
        execute("INSERT INTO users (id) VALUES (?)", userId.toString());
    }

    public void insertAccount(UUID id, UUID userId, String name, long balanceCents, String status) {
        execute("INSERT INTO accounts (id, user_id, name, balance, status) VALUES (?, ?, ?, ?, ?)",
            id.toString(), userId.toString(), name, balanceCents, status);
    }

    public void insertCategory(UUID id, UUID userId, String name, String type, String status) {
        execute("INSERT INTO categories (id, user_id, name, type, status) VALUES (?, ?, ?, ?, ?)",
            id.toString(), userId.toString(), name, type, status);
    }

    public void insertIncome(UUID id, UUID accountId, UUID categoryId, long amountCents, String operationDate,
                             String status) {
        insertOperation("income_operations", id, accountId, categoryId, amountCents, operationDate, status);
    }

    public void insertExpense(UUID id, UUID accountId, UUID categoryId, long amountCents, String operationDate,
                              String status) {
        insertOperation("expense_operations", id, accountId, categoryId, amountCents, operationDate, status);
    }

    public void insertTransfer(UUID id, UUID sourceAccountId, UUID targetAccountId, long amountCents,
                               String operationDate) {
        execute("INSERT INTO transfer_operations (id, source_account_id, target_account_id, amount, operation_date) "
                + "VALUES (?, ?, ?, ?, ?)",
            id.toString(), sourceAccountId.toString(), targetAccountId.toString(), amountCents, operationDate);
    }

    private void insertOperation(String table, UUID id, UUID accountId, UUID categoryId, long amountCents,
                                 String operationDate, String status) {
        execute("INSERT INTO " + table + " (id, account_id, category_id, amount, operation_date, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
            id.toString(), accountId.toString(), categoryId.toString(), amountCents, operationDate, status);
    }

    public void insertObligation(UUID id, UUID userId, String name, long amountCents, UUID accountId, UUID categoryId,
                                 String frequency, String startDate, String endDate, String status) {
        execute("INSERT INTO obligations (id, user_id, name, amount, account_id, category_id, frequency, start_date, "
                + "end_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id.toString(), userId.toString(), name, amountCents, accountId.toString(), categoryId.toString(),
            frequency, startDate, endDate, status);
    }

    public void insertResolution(UUID id, UUID obligationId, String dueDate, String status, UUID expenseId) {
        execute("INSERT INTO occurrence_resolutions (id, obligation_id, due_date, status, expense_id, resolved_at) "
                + "VALUES (?, ?, ?, ?, ?, '2026-10-01T00:00:00Z')",
            id.toString(), obligationId.toString(), dueDate, status, expenseId == null ? null : expenseId.toString());
    }

    public long count(String table) {
        return Long.parseLong(queryFirstColumn("SELECT COUNT(*) FROM " + table));
    }

    /** First column of the first row as text, or {@code null} when there are no rows. */
    public String queryFirstColumn(String sql, Object... params) {
        try (Connection connection = open(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            bind(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void execute(String sql, Object... params) {
        try (Connection connection = open(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            bind(stmt, params);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void bind(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private static InputStream schema() {
        InputStream is = SqliteTestDatabase.class.getClassLoader().getResourceAsStream("db/schema.sql");
        if (is == null) {
            throw new IllegalStateException("db/schema.sql resource not found on classpath");
        }
        return is;
    }
}
