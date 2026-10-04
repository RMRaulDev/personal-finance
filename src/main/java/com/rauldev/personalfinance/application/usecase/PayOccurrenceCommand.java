package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Money;

/**
 * Pays one occurrence of an obligation. {@code amount}, {@code operationDate}, {@code accountId} and
 * {@code categoryId} are optional: {@code null} uses the obligation's expected amount, today, and the obligation's
 * account and category. A supplied amount must be positive; it is recorded as is, even if it differs from the
 * expected amount. A supplied operation date cannot be after today (checked by {@link PayOccurrence}).
 */
public record PayOccurrenceCommand(
    UUID userId,
    UUID obligationId,
    LocalDate dueDate,
    Money amount,
    LocalDate operationDate,
    UUID accountId,
    UUID categoryId
) {
    public PayOccurrenceCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");
        Objects.requireNonNull(dueDate, "Due date cannot be null");
        if (amount != null && !amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
