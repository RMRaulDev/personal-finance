package com.rauldev.personalfinance.application.readmodel;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Money;

/**
 * The expense that paid an occurrence of an obligation.
 */
public record OccurrenceExpense(
    UUID id,
    Money amount,
    LocalDate operationDate
) {
    public OccurrenceExpense {
        Objects.requireNonNull(id, "Expense id cannot be null");
        Objects.requireNonNull(amount, "Expense amount cannot be null");
        Objects.requireNonNull(operationDate, "Expense operation date cannot be null");
    }
}
