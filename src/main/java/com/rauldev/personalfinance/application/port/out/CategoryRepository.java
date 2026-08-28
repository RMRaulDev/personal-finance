package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Category;

public interface CategoryRepository {
    Category create(Category category);

    Optional<Category> findById(UUID id);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    List<Category> findByUserId(UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    Category update(Category category);
}
