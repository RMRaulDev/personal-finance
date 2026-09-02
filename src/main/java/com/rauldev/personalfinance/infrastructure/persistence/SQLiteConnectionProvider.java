package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class SQLiteConnectionProvider {
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
