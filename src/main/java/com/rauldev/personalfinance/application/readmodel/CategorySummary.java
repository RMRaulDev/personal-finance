package com.rauldev.personalfinance.application.readmodel;

import java.util.Objects;
import java.util.UUID;

public record CategorySummary(
    UUID id,
    String name
) {
    public CategorySummary {
        Objects.requireNonNull(id, "Category summary id cannot be null");
        Objects.requireNonNull(name, "Category summary name cannot be null");
    }
}
