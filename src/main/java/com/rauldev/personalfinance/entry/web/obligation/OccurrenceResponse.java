package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;
import java.util.Objects;

import com.rauldev.personalfinance.application.readmodel.ObligationOccurrence;

/**
 * One occurrence of an obligation: {@code {dueDate, status, expense}}. {@code status} is
 * {@code OVERDUE}, {@code PENDING}, {@code PAID}, or {@code SKIPPED}; {@code expense} is the paying
 * expense for {@code PAID} and {@code null} (serialized, not omitted) otherwise.
 */
public record OccurrenceResponse(LocalDate dueDate, String status, OccurrenceExpenseResponse expense) {

    public static OccurrenceResponse from(ObligationOccurrence occurrence) {
        Objects.requireNonNull(occurrence, "Obligation occurrence cannot be null");
        return new OccurrenceResponse(
            occurrence.dueDate(),
            occurrence.status().name(),
            occurrence.expense() == null ? null : OccurrenceExpenseResponse.from(occurrence.expense()));
    }
}
