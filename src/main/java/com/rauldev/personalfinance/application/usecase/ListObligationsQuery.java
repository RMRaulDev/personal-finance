package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

public record ListObligationsQuery(UUID userId) {
    public ListObligationsQuery {
        Objects.requireNonNull(userId, "User id cannot be null");
    }
}
