package com.rauldev.personalfinance.application.readmodel;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One occurrence of an obligation.
 *
 * @param expense the expense that paid it; required when {@code PAID} and {@code null} otherwise
 */
public record ObligationOccurrence(
    LocalDate dueDate,
    OccurrenceStatus status,
    OccurrenceExpense expense
) {
    public ObligationOccurrence {
        Objects.requireNonNull(dueDate, "Due date cannot be null");
        Objects.requireNonNull(status, "Occurrence status cannot be null");
        if (status == OccurrenceStatus.PAID && expense == null) {
            throw new IllegalArgumentException("Paid occurrence requires an expense");
        }
        if (status != OccurrenceStatus.PAID && expense != null) {
            throw new IllegalArgumentException("Only a paid occurrence can have an expense");
        }
    }
}
