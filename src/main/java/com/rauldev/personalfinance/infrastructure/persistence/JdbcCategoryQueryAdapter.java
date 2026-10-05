package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.CategoryQueryPort;
import com.rauldev.personalfinance.application.readmodel.CategoryDetails;
import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcCategoryQueryAdapter implements CategoryQueryPort {
    private final SQLiteConnectionProvider connectionProvider;
    private final TransactionConnectionHolder connectionHolder;

    public JdbcCategoryQueryAdapter(
        SQLiteConnectionProvider connectionProvider,
        TransactionConnectionHolder connectionHolder
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "Connection provider cannot be null");
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public List<CategoryDetails> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT id, name, type, status FROM categories WHERE user_id = ? ORDER BY name, id";

        if (connectionHolder.hasActiveTransaction()) {
            Connection connection = connectionHolder.get();
            return executeQuery(connection, sql, userId);
        } else {
            try (Connection connection = connectionProvider.getConnection()) {
                return executeQuery(connection, sql, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to query categories", e);
            }
        }
    }

    private List<CategoryDetails> executeQuery(Connection connection, String sql, UUID userId) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CategoryDetails> categories = new ArrayList<>();
                while (resultSet.next()) {
                    categories.add(mapRow(resultSet));
                }
                return categories;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query categories", e);
        }
    }

    private static CategoryDetails mapRow(ResultSet resultSet) throws SQLException {
        String rowId = resultSet.getString("id");
        try {
            UUID id = UUID.fromString(resultSet.getString("id"));
            String name = resultSet.getString("name");
            CategoryType type = CategoryType.valueOf(resultSet.getString("type"));
            CategoryStatus status = CategoryStatus.valueOf(resultSet.getString("status"));

            return new CategoryDetails(id, name, type, status);
        } catch (RuntimeException e) {
            throw new CorruptedPersistedDataException("categories", rowId, e);
        }
    }
}
