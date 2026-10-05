package com.rauldev.personalfinance.application.readmodel;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.CategoryType;

public record CategoryDetails(
    UUID id,
    String name,
    CategoryType type,
    CategoryStatus status
) {
    public CategoryDetails {
        Objects.requireNonNull(id, "Category id cannot be null");
        Objects.requireNonNull(name, "Category name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        Objects.requireNonNull(type, "Category type cannot be null");
        Objects.requireNonNull(status, "Category status cannot be null");
    }
}
