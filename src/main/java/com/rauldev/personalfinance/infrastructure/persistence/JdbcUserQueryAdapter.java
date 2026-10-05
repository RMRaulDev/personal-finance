package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.UserQueryPort;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcUserQueryAdapter implements UserQueryPort {
    private final SQLiteConnectionProvider connectionProvider;
    private final TransactionConnectionHolder connectionHolder;

    public JdbcUserQueryAdapter(
        SQLiteConnectionProvider connectionProvider,
        TransactionConnectionHolder connectionHolder
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "Connection provider cannot be null");
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public boolean existsById(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT 1 FROM users WHERE id = ?";

        if (connectionHolder.hasActiveTransaction()) {
            Connection connection = connectionHolder.get();
            return executeQuery(connection, sql, userId);
        } else {
            try (Connection connection = connectionProvider.getConnection()) {
                return executeQuery(connection, sql, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to query user existence", e);
            }
        }
    }

    private boolean executeQuery(Connection connection, String sql, UUID userId) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query user existence", e);
        }
    }
}
