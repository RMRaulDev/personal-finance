package com.rauldev.personalfinance.application.readmodel;

import java.util.Objects;
import java.util.UUID;

public record AccountSummary(
    UUID id,
    String name
) {
    public AccountSummary {
        Objects.requireNonNull(id, "Account summary id cannot be null");
        Objects.requireNonNull(name, "Account summary name cannot be null");
    }
}
