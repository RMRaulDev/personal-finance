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

import com.rauldev.personalfinance.application.port.out.IncomeOperationRepository;
import com.rauldev.personalfinance.domain.Income;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcIncomeOperationRepository implements IncomeOperationRepository {
    private final TransactionConnectionHolder connectionHolder;

    public JdbcIncomeOperationRepository(TransactionConnectionHolder connectionHolder) {
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public Income create(Income income) {
        Objects.requireNonNull(income, "Income cannot be null");

        String sql = "INSERT INTO income_operations (id, account_id, category_id, amount, operation_date, status) VALUES (?, ?, ?, ?, ?, ?)";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, income.id().toString());
            statement.setString(2, income.accountId().toString());
            statement.setString(3, income.categoryId().toString());
            statement.setLong(4, toCents(income.amount()));
            statement.setString(5, income.operationDate().toString());
            statement.setString(6, income.status().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create income operation", e);
        }

        return income;
    }

    @Override
    public Optional<Income> findById(UUID id) {
        Objects.requireNonNull(id, "Income id cannot be null");

        String sql = "SELECT io.id, a.user_id, io.account_id, io.category_id, io.amount, io.operation_date, io.status "
            + "FROM income_operations io "
            + "JOIN accounts a ON a.id = io.account_id "
            + "WHERE io.id = ?";
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
            throw new RuntimeException("Failed to find income operation by id", e);
        }
    }

    @Override
    public Optional<Income> findByIdAndUserId(UUID id, UUID userId) {
        Objects.requireNonNull(id, "Income id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT io.id, a.user_id, io.account_id, io.category_id, io.amount, io.operation_date, io.status "
            + "FROM income_operations io "
            + "JOIN accounts a ON a.id = io.account_id "
            + "WHERE io.id = ? AND a.user_id = ?";
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
            throw new RuntimeException("Failed to find income operation by id and user id", e);
        }
    }

    @Override
    public List<Income> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT io.id, a.user_id, io.account_id, io.category_id, io.amount, io.operation_date, io.status "
            + "FROM income_operations io "
            + "JOIN accounts a ON a.id = io.account_id "
            + "WHERE a.user_id = ? "
            + "ORDER BY io.operation_date DESC, io.id DESC";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Income> incomes = new ArrayList<>();
                while (resultSet.next()) {
                    incomes.add(mapRow(resultSet));
                }
                return incomes;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find income operations by user id", e);
        }
    }

    @Override
    public Income update(Income income) {
        Objects.requireNonNull(income, "Income cannot be null");

        String sql = "UPDATE income_operations SET status = ? WHERE id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, income.status().name());
            statement.setString(2, income.id().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update income operation", e);
        }

        return income;
    }

    @Override
    public void deleteById(UUID id) {
        Objects.requireNonNull(id, "Income id cannot be null");

        String sql = "DELETE FROM income_operations WHERE id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete income operation by id", e);
        }
    }

    private static Income mapRow(ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        UUID userId = UUID.fromString(resultSet.getString("user_id"));
        UUID accountId = UUID.fromString(resultSet.getString("account_id"));
        UUID categoryId = UUID.fromString(resultSet.getString("category_id"));
        Money amount = Money.ofCents(resultSet.getLong("amount"));
        LocalDate operationDate = LocalDate.parse(resultSet.getString("operation_date"));

        Income income = new Income(id, userId, amount, operationDate, accountId, categoryId);

        OperationStatus status = OperationStatus.valueOf(resultSet.getString("status"));
        if (status == OperationStatus.CANCELLED) {
            income.cancel();
        }

        return income;
    }

    private static long toCents(Money money) {
        return money.amount().movePointRight(2).longValueExact();
    }
}
