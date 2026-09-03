package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.TransferOperationRepository;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Transfer;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcTransferOperationRepository implements TransferOperationRepository {
    private final TransactionConnectionHolder connectionHolder;

    public JdbcTransferOperationRepository(TransactionConnectionHolder connectionHolder) {
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public Transfer create(Transfer transfer) {
        Objects.requireNonNull(transfer, "Transfer cannot be null");

        String sql = "INSERT INTO transfer_operations (id, source_account_id, target_account_id, amount, operation_date) VALUES (?, ?, ?, ?, ?)";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transfer.id().toString());
            statement.setString(2, transfer.sourceAccountId().toString());
            statement.setString(3, transfer.targetAccountId().toString());
            statement.setLong(4, toCents(transfer.amount()));
            statement.setString(5, transfer.operationDate().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create transfer operation", e);
        }

        return transfer;
    }

    @Override
    public Optional<Transfer> findById(UUID id) {
        Objects.requireNonNull(id, "Transfer id cannot be null");

        String sql = "SELECT t.id, a.user_id, t.source_account_id, t.target_account_id, t.amount, t.operation_date "
            + "FROM transfer_operations t "
            + "JOIN accounts a ON a.id = t.source_account_id "
            + "WHERE t.id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find transfer operation by id", e);
        }
    }

    @Override
    public Optional<Transfer> findByIdAndUserId(UUID id, UUID userId) {
        Objects.requireNonNull(id, "Transfer id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT t.id, a.user_id, t.source_account_id, t.target_account_id, t.amount, t.operation_date "
            + "FROM transfer_operations t "
            + "JOIN accounts a ON a.id = t.source_account_id "
            + "WHERE t.id = ? AND a.user_id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.setString(2, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find transfer operation by id and user id", e);
        }
    }

    @Override
    public List<Transfer> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT t.id, a.user_id, t.source_account_id, t.target_account_id, t.amount, t.operation_date "
            + "FROM transfer_operations t "
            + "JOIN accounts a ON a.id = t.source_account_id "
            + "WHERE a.user_id = ? "
            + "ORDER BY t.operation_date DESC, t.id DESC";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Transfer> transfers = new ArrayList<>();
                while (resultSet.next()) {
                    transfers.add(mapRow(resultSet));
                }
                return transfers;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find transfer operations by user id", e);
        }
    }

    @Override
    public void deleteById(UUID id) {
        Objects.requireNonNull(id, "Transfer id cannot be null");

        String sql = "DELETE FROM transfer_operations WHERE id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete transfer operation by id", e);
        }
    }

    private static Transfer mapRow(ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        UUID userId = UUID.fromString(resultSet.getString("user_id"));
        UUID sourceAccountId = UUID.fromString(resultSet.getString("source_account_id"));
        UUID targetAccountId = UUID.fromString(resultSet.getString("target_account_id"));
        Money amount = Money.ofCents(resultSet.getLong("amount"));
        LocalDate operationDate = LocalDate.parse(resultSet.getString("operation_date"));

        return new Transfer(id, userId, amount, operationDate, sourceAccountId, targetAccountId);
    }

    private static long toCents(Money money) {
        return money.amount().movePointRight(2).longValueExact();
    }
}
