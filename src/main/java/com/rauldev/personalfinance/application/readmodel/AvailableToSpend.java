package com.rauldev.personalfinance.application.readmodel;

import java.util.Objects;

import com.rauldev.personalfinance.domain.Money;

/**
 * Balance of the active accounts against the committed amount. At most one of {@code available} and
 * {@code shortfall} is positive, and {@code balance + shortfall == committed + available}.
 */
public record AvailableToSpend(
    Money balance,
    Money committed,
    Money available,
    Money shortfall
) {
    public AvailableToSpend {
        Objects.requireNonNull(balance, "Balance cannot be null");
        Objects.requireNonNull(committed, "Committed amount cannot be null");
        Objects.requireNonNull(available, "Available amount cannot be null");
        Objects.requireNonNull(shortfall, "Shortfall cannot be null");
        if (available.isPositive() && shortfall.isPositive()) {
            throw new IllegalArgumentException("Available and shortfall cannot both be positive");
        }
        if (balance.add(shortfall).compareTo(committed.add(available)) != 0) {
            throw new IllegalArgumentException("Available and shortfall are inconsistent with balance and committed");
        }
    }
}
