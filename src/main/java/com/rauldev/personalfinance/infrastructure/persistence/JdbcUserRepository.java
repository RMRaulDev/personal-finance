package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.UserRepository;
import com.rauldev.personalfinance.domain.User;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcUserRepository implements UserRepository {
    private final TransactionConnectionHolder connectionHolder;

    public JdbcUserRepository(TransactionConnectionHolder connectionHolder) {
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public User create(User user) {
        Objects.requireNonNull(user, "User cannot be null");

        String sql = "INSERT INTO users (id) VALUES (?)";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.id().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create user", e);
        }

        return user;
    }

    @Override
    public void deleteById(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "DELETE FROM users WHERE id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user by id", e);
        }
    }
}
