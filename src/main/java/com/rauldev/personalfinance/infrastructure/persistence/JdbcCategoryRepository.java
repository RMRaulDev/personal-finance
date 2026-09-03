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

import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcCategoryRepository implements CategoryRepository {
    private final TransactionConnectionHolder connectionHolder;

    public JdbcCategoryRepository(TransactionConnectionHolder connectionHolder) {
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public Category create(Category category) {
        Objects.requireNonNull(category, "Category cannot be null");

        String sql = "INSERT INTO categories (id, user_id, name, type, status) VALUES (?, ?, ?, ?, ?)";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.id().toString());
            statement.setString(2, category.userId().toString());
            statement.setString(3, category.name());
            statement.setString(4, category.type().name());
            statement.setString(5, category.status().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create category", e);
        }

        return category;
    }

    @Override
    public Optional<Category> findById(UUID id) {
        Objects.requireNonNull(id, "Category id cannot be null");

        String sql = "SELECT id, user_id, name, type, status FROM categories WHERE id = ?";
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
            throw new RuntimeException("Failed to find category by id", e);
        }
    }

    @Override
    public Optional<Category> findByIdAndUserId(UUID id, UUID userId) {
        Objects.requireNonNull(id, "Category id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT id, user_id, name, type, status FROM categories WHERE id = ? AND user_id = ?";
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
            throw new RuntimeException("Failed to find category by id and user id", e);
        }
    }

    @Override
    public List<Category> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = "SELECT id, user_id, name, type, status FROM categories WHERE user_id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Category> categories = new ArrayList<>();
                while (resultSet.next()) {
                    categories.add(mapRow(resultSet));
                }
                return categories;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find categories by user id", e);
        }
    }

    @Override
    public boolean existsByUserIdAndName(UUID userId, String name) {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(name, "Category name cannot be null");

        String sql = "SELECT 1 FROM categories WHERE user_id = ? AND name = ? LIMIT 1";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check category existence by user id and name", e);
        }
    }

    @Override
    public Category update(Category category) {
        Objects.requireNonNull(category, "Category cannot be null");

        String sql = "UPDATE categories SET name = ?, status = ? WHERE id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.name());
            statement.setString(2, category.status().name());
            statement.setString(3, category.id().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update category", e);
        }

        return category;
    }

    private static Category mapRow(ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        UUID userId = UUID.fromString(resultSet.getString("user_id"));
        String name = resultSet.getString("name");
        CategoryType type = CategoryType.valueOf(resultSet.getString("type"));

        Category category = new Category(id, userId, name, type);

        CategoryStatus status = CategoryStatus.valueOf(resultSet.getString("status"));
        if (status == CategoryStatus.INACTIVE) {
            category.deactivate();
        }

        return category;
    }
}
