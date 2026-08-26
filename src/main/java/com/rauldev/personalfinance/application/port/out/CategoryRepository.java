package com.rauldev.personalfinance.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryType;

public interface CategoryRepository {
    Optional<Category> findById(UUID id);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNameAndType(UUID userId, String name, CategoryType type);

    void save(Category category);
}
