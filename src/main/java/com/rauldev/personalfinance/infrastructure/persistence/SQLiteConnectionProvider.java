package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Opens SQLite connections with the pragmas the application relies on.
 *
 * <p>Every connection enables foreign keys and sets an explicit busy timeout: a connection that
 * finds the database locked by another connection waits up to {@value #BUSY_TIMEOUT_MILLIS} ms
 * before failing with {@code SQLITE_BUSY}.
 */
public final class SQLiteConnectionProvider {
    static final int BUSY_TIMEOUT_MILLIS = 5000;

    private final String jdbcUrl;

    public SQLiteConnectionProvider(String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "JDBC URL cannot be null");
        if (!jdbcUrl.startsWith("jdbc:sqlite:")) {
            throw new IllegalArgumentException("JDBC URL must start with 'jdbc:sqlite:'");
        }
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
            statement.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MILLIS + ";");
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
        return connection;
    }
}
