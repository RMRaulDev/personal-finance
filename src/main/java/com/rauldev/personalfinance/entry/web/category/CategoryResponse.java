package com.rauldev.personalfinance.entry.web.category;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.CategoryDetails;

/**
 * Element of {@code GET /api/v1/categories}. The type ({@code INCOME}, {@code EXPENSE}) and status
 * ({@code ACTIVE}, {@code INACTIVE}) are the enum constant names.
 */
public record CategoryResponse(UUID id, String name, String type, String status) {

    public static CategoryResponse from(CategoryDetails category) {
        Objects.requireNonNull(category, "Category details cannot be null");
        return new CategoryResponse(
            category.id(), category.name(), category.type().name(), category.status().name());
    }
}
