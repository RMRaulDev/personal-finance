package com.rauldev.personalfinance.application.readmodel;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;

/**
 * Read model of an obligation with the names of its payment account and category. The calendar is the immutable
 * {@link Recurrence} value object (as {@link Money} is for the amount); its {@code endDate()} is empty when the
 * calendar has no end.
 */
public record ObligationDetails(
    UUID id,
    String name,
    Money amount,
    AccountSummary account,
    CategorySummary category,
    Recurrence recurrence,
    ObligationStatus status
) {
    public ObligationDetails {
        Objects.requireNonNull(id, "Obligation id cannot be null");
        Objects.requireNonNull(name, "Obligation name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Obligation name cannot be empty");
        }
        Objects.requireNonNull(amount, "Obligation amount cannot be null");
        Objects.requireNonNull(account, "Obligation account cannot be null");
        Objects.requireNonNull(category, "Obligation category cannot be null");
        Objects.requireNonNull(recurrence, "Obligation recurrence cannot be null");
        Objects.requireNonNull(status, "Obligation status cannot be null");
    }
}
