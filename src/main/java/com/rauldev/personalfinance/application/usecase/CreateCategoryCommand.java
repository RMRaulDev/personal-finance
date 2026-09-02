package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.CategoryType;

public record CreateCategoryCommand(
    UUID userId,
    String name,
    CategoryType type
) {
    public CreateCategoryCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(name, "Category name cannot be null");
        Objects.requireNonNull(type, "Category type cannot be null");
    }
}
