package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.ReversalRepository;
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.FinancialOperation;
import com.rauldev.personalfinance.domain.Income;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;
import com.rauldev.personalfinance.domain.Reversal;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcReversalRepository implements ReversalRepository {
    private final TransactionConnectionHolder connectionHolder;

    public JdbcReversalRepository(TransactionConnectionHolder connectionHolder) {
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public Reversal create(Reversal reversal) {
        Objects.requireNonNull(reversal, "Reversal cannot be null");

        String sql = "INSERT INTO reversals (id, original_operation_id, cancelled_at) VALUES (?, ?, ?)";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reversal.id().toString());
            statement.setString(2, reversal.originalOperationId().toString());
            statement.setString(3, reversal.cancelledAt().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create reversal", e);
        }

        return reversal;
    }

    @Override
    public Optional<Reversal> findById(UUID id) {
        Objects.requireNonNull(id, "Reversal id cannot be null");

        String sql = "SELECT r.id AS reversal_id, r.original_operation_id, r.cancelled_at, 'INCOME' AS op_type, "
            + "io.id AS op_id, a.user_id, io.account_id, io.category_id, io.amount, io.operation_date, io.status "
            + "FROM reversals r "
            + "JOIN income_operations io ON io.id = r.original_operation_id "
            + "JOIN accounts a ON a.id = io.account_id "
            + "WHERE r.id = ? "
            + "UNION ALL "
            + "SELECT r.id AS reversal_id, r.original_operation_id, r.cancelled_at, 'EXPENSE' AS op_type, "
            + "eo.id AS op_id, a.user_id, eo.account_id, eo.category_id, eo.amount, eo.operation_date, eo.status "
            + "FROM reversals r "
            + "JOIN expense_operations eo ON eo.id = r.original_operation_id "
            + "JOIN accounts a ON a.id = eo.account_id "
            + "WHERE r.id = ?";

        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.setString(2, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reversal by id", e);
        }

        if (existsById(connection, id)) {
            throw new RuntimeException("Failed to reconstruct reversal: original operation not found");
        }

        return Optional.empty();
    }

    @Override
    public Optional<Reversal> findByOriginalOperationId(UUID operationId) {
        Objects.requireNonNull(operationId, "Original operation id cannot be null");

        String sql = "SELECT r.id AS reversal_id, r.original_operation_id, r.cancelled_at, 'INCOME' AS op_type, "
            + "io.id AS op_id, a.user_id, io.account_id, io.category_id, io.amount, io.operation_date, io.status "
            + "FROM reversals r "
            + "JOIN income_operations io ON io.id = r.original_operation_id "
            + "JOIN accounts a ON a.id = io.account_id "
            + "WHERE r.original_operation_id = ? "
            + "UNION ALL "
            + "SELECT r.id AS reversal_id, r.original_operation_id, r.cancelled_at, 'EXPENSE' AS op_type, "
            + "eo.id AS op_id, a.user_id, eo.account_id, eo.category_id, eo.amount, eo.operation_date, eo.status "
            + "FROM reversals r "
            + "JOIN expense_operations eo ON eo.id = r.original_operation_id "
            + "JOIN accounts a ON a.id = eo.account_id "
            + "WHERE r.original_operation_id = ?";

        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, operationId.toString());
            statement.setString(2, operationId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reversal by original operation id", e);
        }

        if (existsByOriginalOperationId(connection, operationId)) {
            throw new RuntimeException("Failed to reconstruct reversal: original operation not found");
        }

        return Optional.empty();
    }

    @Override
    public void deleteById(UUID id) {
        Objects.requireNonNull(id, "Reversal id cannot be null");

        String sql = "DELETE FROM reversals WHERE id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete reversal by id", e);
        }
    }

    private static boolean existsById(Connection connection, UUID id) {
        String sql = "SELECT 1 FROM reversals WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check reversal existence by id", e);
        }
    }

    private static boolean existsByOriginalOperationId(Connection connection, UUID operationId) {
        String sql = "SELECT 1 FROM reversals WHERE original_operation_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, operationId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check reversal existence by original operation id", e);
        }
    }

    private static Reversal mapRow(ResultSet resultSet) throws SQLException {
        UUID reversalId = UUID.fromString(resultSet.getString("reversal_id"));
        Instant cancelledAt = Instant.parse(resultSet.getString("cancelled_at"));
        String opType = resultSet.getString("op_type");

        UUID opId = UUID.fromString(resultSet.getString("op_id"));
        UUID userId = UUID.fromString(resultSet.getString("user_id"));
        UUID accountId = UUID.fromString(resultSet.getString("account_id"));
        UUID categoryId = UUID.fromString(resultSet.getString("category_id"));
        Money amount = Money.ofCents(resultSet.getLong("amount"));
        LocalDate operationDate = LocalDate.parse(resultSet.getString("operation_date"));
        OperationStatus status = OperationStatus.valueOf(resultSet.getString("status"));

        FinancialOperation originalOperation;
        if ("INCOME".equals(opType)) {
            Income income = new Income(opId, userId, amount, operationDate, accountId, categoryId);
            if (status == OperationStatus.CANCELLED) {
                income.cancel();
            }
            originalOperation = income;
        } else if ("EXPENSE".equals(opType)) {
            Expense expense = new Expense(opId, userId, amount, operationDate, accountId, categoryId);
            if (status == OperationStatus.CANCELLED) {
                expense.cancel();
            }
            originalOperation = expense;
        } else {
            throw new IllegalStateException("Unknown operation type: " + opType);
        }

        return new Reversal(reversalId, originalOperation, cancelledAt);
    }
}
