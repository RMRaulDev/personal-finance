package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.AccountQueryPort;
import com.rauldev.personalfinance.application.readmodel.AccountDetails;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcAccountQueryAdapter implements AccountQueryPort {
    private final SQLiteConnectionProvider connectionProvider;
    private final TransactionConnectionHolder connectionHolder;

    public JdbcAccountQueryAdapter(
        SQLiteConnectionProvider connectionProvider,
        TransactionConnectionHolder connectionHolder
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "Connection provider cannot be null");
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public Optional<AccountDetails> findByIdAndUserId(UUID accountId, UUID userId) {
        Objects.requireNonNull(accountId, "Account id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT id, user_id, name, balance, status FROM accounts WHERE id = ? AND user_id = ?";

        if (connectionHolder.hasActiveTransaction()) {
            Connection connection = connectionHolder.get();
            return executeQuery(connection, sql, accountId, userId);
        } else {
            try (Connection connection = connectionProvider.getConnection()) {
                return executeQuery(connection, sql, accountId, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to query account details", e);
            }
        }
    }

    @Override
    public List<AccountDetails> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT id, user_id, name, balance, status FROM accounts WHERE user_id = ? ORDER BY name, id";

        if (connectionHolder.hasActiveTransaction()) {
            Connection connection = connectionHolder.get();
            return executeListQuery(connection, sql, userId);
        } else {
            try (Connection connection = connectionProvider.getConnection()) {
                return executeListQuery(connection, sql, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to query accounts", e);
            }
        }
    }

    private Optional<AccountDetails> executeQuery(Connection connection, String sql, UUID accountId, UUID userId) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountId.toString());
            statement.setString(2, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query account details", e);
        }
    }

    private List<AccountDetails> executeListQuery(Connection connection, String sql, UUID userId) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AccountDetails> accounts = new ArrayList<>();
                while (resultSet.next()) {
                    accounts.add(mapRow(resultSet));
                }
                return accounts;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query accounts", e);
        }
    }

    private static AccountDetails mapRow(ResultSet resultSet) throws SQLException {
        String rowId = resultSet.getString("id");
        try {
            UUID id = UUID.fromString(resultSet.getString("id"));
            UUID userId = UUID.fromString(resultSet.getString("user_id"));
            String name = resultSet.getString("name");
            Money balance = Money.ofCents(resultSet.getLong("balance"));
            AccountStatus status = AccountStatus.valueOf(resultSet.getString("status"));

            return new AccountDetails(id, userId, name, balance, status);
        } catch (RuntimeException e) {
            throw new CorruptedPersistedDataException("accounts", rowId, e);
        }
    }
}
