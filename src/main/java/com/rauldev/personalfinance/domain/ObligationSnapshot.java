package com.rauldev.personalfinance.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Read-only obligation data consumed by {@link CommitmentCalculator}. It lets read-side callers pass rows read with
 * SQL without reconstructing the {@link Obligation} aggregate.
 */
public record ObligationSnapshot(UUID id, String name, Money amount, UUID accountId, UUID categoryId,
                                 Recurrence recurrence, ObligationStatus status) {

    public ObligationSnapshot {
        Objects.requireNonNull(id, "Obligation id cannot be null");
        Objects.requireNonNull(name, "Obligation name cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Obligation amount must be greater than zero");
        }
        Objects.requireNonNull(accountId, "Account id cannot be null");
        Objects.requireNonNull(categoryId, "Category id cannot be null");
        Objects.requireNonNull(recurrence, "Recurrence cannot be null");
        Objects.requireNonNull(status, "Obligation status cannot be null");
    }
}
