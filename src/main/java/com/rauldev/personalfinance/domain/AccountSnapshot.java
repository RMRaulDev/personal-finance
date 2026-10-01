package com.rauldev.personalfinance.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Read-only account data consumed by {@link CommitmentCalculator}. It lets read-side callers pass rows read with SQL
 * without reconstructing the {@link Account} aggregate.
 */
public record AccountSnapshot(UUID id, String name, Money balance, AccountStatus status) {

    public AccountSnapshot {
        Objects.requireNonNull(id, "Account id cannot be null");
        Objects.requireNonNull(name, "Account name cannot be null");
        Objects.requireNonNull(balance, "Balance cannot be null");
        Objects.requireNonNull(status, "Account status cannot be null");
    }
}
