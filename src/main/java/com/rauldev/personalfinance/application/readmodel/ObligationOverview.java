package com.rauldev.personalfinance.application.readmodel;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.ObligationTimeline;

/**
 * An obligation with the state of its occurrences on a given day, computed with {@link ObligationTimeline}.
 *
 * @param nextDueDate the oldest unresolved occurrence (the oldest overdue date if any, otherwise the next
 *                    unresolved date on or after today); {@code null} when there is none or the obligation is
 *                    archived
 * @param overdueCount the number of overdue occurrences; zero when the obligation is archived
 */
public record ObligationOverview(
    ObligationDetails obligation,
    LocalDate nextDueDate,
    long overdueCount
) {
    public ObligationOverview {
        Objects.requireNonNull(obligation, "Obligation cannot be null");
        if (overdueCount < 0) {
            throw new IllegalArgumentException("Overdue count cannot be negative");
        }
        if (obligation.status() == ObligationStatus.ARCHIVED && (nextDueDate != null || overdueCount > 0)) {
            throw new IllegalArgumentException(
                "An archived obligation cannot have a next due date or overdue occurrences");
        }
    }

    /**
     * @param resolvedDates the due dates of every resolution of the obligation
     */
    public static ObligationOverview of(ObligationDetails obligation, Set<LocalDate> resolvedDates, LocalDate today) {
        Objects.requireNonNull(obligation, "Obligation cannot be null");
        ObligationTimeline timeline = new ObligationTimeline(obligation.recurrence(), obligation.status(),
            resolvedDates);
        return new ObligationOverview(obligation, timeline.nextDueDate(today).orElse(null),
            timeline.overdueCount(today));
    }
}
