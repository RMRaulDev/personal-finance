package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

public record GetDashboardQuery(
    UUID userId
) {
    public GetDashboardQuery {
        Objects.requireNonNull(userId, "User id cannot be null");
    }
}
