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

import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcExpenseOperationRepository implements ExpenseOperationRepository {
    private final TransactionConnectionHolder connectionHolder;

    public JdbcExpenseOperationRepository(TransactionConnectionHolder connectionHolder) {
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public Expense create(Expense expense) {
        Objects.requireNonNull(expense, "Expense cannot be null");

        String sql = "INSERT INTO expense_operations (id, account_id, category_id, amount, operation_date, status) VALUES (?, ?, ?, ?, ?, ?)";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, expense.id().toString());
            statement.setString(2, expense.accountId().toString());
            statement.setString(3, expense.categoryId().toString());
            statement.setLong(4, toCents(expense.amount()));
            statement.setString(5, expense.operationDate().toString());
            statement.setString(6, expense.status().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create expense operation", e);
        }

        return expense;
    }

    @Override
    public Optional<Expense> findById(UUID id) {
        Objects.requireNonNull(id, "Expense id cannot be null");

        String sql = "SELECT eo.id, a.user_id, eo.account_id, eo.category_id, eo.amount, eo.operation_date, eo.status "
            + "FROM expense_operations eo "
            + "JOIN accounts a ON a.id = eo.account_id "
            + "WHERE eo.id = ?";
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
            throw new RuntimeException("Failed to find expense operation by id", e);
        }
    }

    @Override
    public Optional<Expense> findByIdAndUserId(UUID id, UUID userId) {
        Objects.requireNonNull(id, "Expense id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT eo.id, a.user_id, eo.account_id, eo.category_id, eo.amount, eo.operation_date, eo.status "
            + "FROM expense_operations eo "
            + "JOIN accounts a ON a.id = eo.account_id "
            + "WHERE eo.id = ? AND a.user_id = ?";
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
            throw new RuntimeException("Failed to find expense operation by id and user id", e);
        }
    }

    @Override
    public List<Expense> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT eo.id, a.user_id, eo.account_id, eo.category_id, eo.amount, eo.operation_date, eo.status "
            + "FROM expense_operations eo "
            + "JOIN accounts a ON a.id = eo.account_id "
            + "WHERE a.user_id = ? "
            + "ORDER BY eo.operation_date DESC, eo.id DESC";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Expense> expenses = new ArrayList<>();
                while (resultSet.next()) {
                    expenses.add(mapRow(resultSet));
                }
                return expenses;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find expense operations by user id", e);
        }
    }

    @Override
    public Expense update(Expense expense) {
        Objects.requireNonNull(expense, "Expense cannot be null");

        String sql = "UPDATE expense_operations SET status = ? WHERE id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, expense.status().name());
            statement.setString(2, expense.id().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update expense operation", e);
        }

        return expense;
    }

    @Override
    public void deleteById(UUID id) {
        Objects.requireNonNull(id, "Expense id cannot be null");

        String sql = "DELETE FROM expense_operations WHERE id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete expense operation by id", e);
        }
    }

    private static Expense mapRow(ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        UUID userId = UUID.fromString(resultSet.getString("user_id"));
        UUID accountId = UUID.fromString(resultSet.getString("account_id"));
        UUID categoryId = UUID.fromString(resultSet.getString("category_id"));
        Money amount = Money.ofCents(resultSet.getLong("amount"));
        LocalDate operationDate = LocalDate.parse(resultSet.getString("operation_date"));

        Expense expense = new Expense(id, userId, amount, operationDate, accountId, categoryId);

        OperationStatus status = OperationStatus.valueOf(resultSet.getString("status"));
        if (status == OperationStatus.CANCELLED) {
            expense.cancel();
        }

        return expense;
    }

    private static long toCents(Money money) {
        return money.amount().movePointRight(2).longValueExact();
    }
}
