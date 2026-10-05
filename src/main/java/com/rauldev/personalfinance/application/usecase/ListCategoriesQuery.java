package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

public record ListCategoriesQuery(UUID userId) {
    public ListCategoriesQuery {
        Objects.requireNonNull(userId, "User id cannot be null");
    }
}
