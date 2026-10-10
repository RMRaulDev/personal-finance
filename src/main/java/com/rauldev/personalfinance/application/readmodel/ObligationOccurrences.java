package com.rauldev.personalfinance.application.readmodel;

import java.util.List;
import java.util.Objects;

/**
 * The occurrences of one obligation on a given day.
 *
 * @param overdue every overdue occurrence, ascending by due date
 * @param upcoming the next unresolved occurrences on or after today, ascending by due date
 * @param resolved the most recent resolutions (paid or skipped, advance payments included), descending by due date
 */
public record ObligationOccurrences(
    List<ObligationOccurrence> overdue,
    List<ObligationOccurrence> upcoming,
    List<ObligationOccurrence> resolved
) {
    public ObligationOccurrences {
        overdue = List.copyOf(Objects.requireNonNull(overdue, "Overdue occurrences cannot be null"));
        upcoming = List.copyOf(Objects.requireNonNull(upcoming, "Upcoming occurrences cannot be null"));
        resolved = List.copyOf(Objects.requireNonNull(resolved, "Resolved occurrences cannot be null"));
    }
}
