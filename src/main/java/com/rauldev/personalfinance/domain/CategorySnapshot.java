package com.rauldev.personalfinance.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Read-only category data consumed by {@link CommitmentCalculator}.
 */
public record CategorySnapshot(UUID id, CategoryStatus status) {

    public CategorySnapshot {
        Objects.requireNonNull(id, "Category id cannot be null");
        Objects.requireNonNull(status, "Category status cannot be null");
    }
}
