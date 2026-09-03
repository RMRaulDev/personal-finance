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

import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcAccountRepository implements AccountRepository {
    private final TransactionConnectionHolder connectionHolder;

    public JdbcAccountRepository(TransactionConnectionHolder connectionHolder) {
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public Account create(Account account) {
        Objects.requireNonNull(account, "Account cannot be null");

        String sql = "INSERT INTO accounts (id, user_id, name, balance, status) VALUES (?, ?, ?, ?, ?)";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.id().toString());
            statement.setString(2, account.userId().toString());
            statement.setString(3, account.name());
            statement.setLong(4, toCents(account.balance()));
            statement.setString(5, account.status().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create account", e);
        }

        return account;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        Objects.requireNonNull(id, "Account id cannot be null");

        String sql = "SELECT id, user_id, name, balance, status FROM accounts WHERE id = ?";
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
            throw new RuntimeException("Failed to find account by id", e);
        }
    }

    @Override
    public Optional<Account> findByIdAndUserId(UUID id, UUID userId) {
        Objects.requireNonNull(id, "Account id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT id, user_id, name, balance, status FROM accounts WHERE id = ? AND user_id = ?";
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
            throw new RuntimeException("Failed to find account by id and user id", e);
        }
    }

    @Override
    public List<Account> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT id, user_id, name, balance, status FROM accounts WHERE user_id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Account> accounts = new ArrayList<>();
                while (resultSet.next()) {
                    accounts.add(mapRow(resultSet));
                }
                return accounts;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find accounts by user id", e);
        }
    }

    @Override
    public boolean existsByUserIdAndName(UUID userId, String name) {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(name, "Account name cannot be null");

        String sql = "SELECT 1 FROM accounts WHERE user_id = ? AND name = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check account existence by user id and name", e);
        }
    }

    @Override
    public Account update(Account account) {
        Objects.requireNonNull(account, "Account cannot be null");

        String sql = "UPDATE accounts SET name = ?, balance = ?, status = ? WHERE id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.name());
            statement.setLong(2, toCents(account.balance()));
            statement.setString(3, account.status().name());
            statement.setString(4, account.id().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update account", e);
        }

        return account;
    }

    private static Account mapRow(ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        UUID userId = UUID.fromString(resultSet.getString("user_id"));
        String name = resultSet.getString("name");
        Account account = new Account(id, userId, name);

        Money balance = Money.ofCents(resultSet.getLong("balance"));
        if (balance.isPositive()) {
            account.credit(balance);
        }

        AccountStatus status = AccountStatus.valueOf(resultSet.getString("status"));
        if (status == AccountStatus.INACTIVE) {
            account.deactivate();
        }

        return account;
    }

    private static long toCents(Money money) {
        return money.amount().movePointRight(2).longValueExact();
    }
}
