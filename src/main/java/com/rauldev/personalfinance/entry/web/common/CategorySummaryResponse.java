package com.rauldev.personalfinance.entry.web.common;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.CategorySummary;

public record CategorySummaryResponse(UUID id, String name) {

    public static CategorySummaryResponse from(CategorySummary category) {
        Objects.requireNonNull(category, "Category summary cannot be null");
        return new CategorySummaryResponse(category.id(), category.name());
    }
}
