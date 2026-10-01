package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-only resolution data consumed by {@link CommitmentCalculator}: only which occurrence was resolved.
 */
public record ResolutionSnapshot(UUID obligationId, LocalDate dueDate) {

    public ResolutionSnapshot {
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");
        Objects.requireNonNull(dueDate, "Due date cannot be null");
    }
}
