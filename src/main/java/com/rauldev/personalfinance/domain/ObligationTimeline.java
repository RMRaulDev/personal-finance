package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Pure view of the occurrences of an obligation on a given day, for read-side callers that hold its calendar, its
 * status, and the due dates of all its resolutions instead of the {@link Obligation} aggregate.
 *
 * Overdue occurrences have the same definition as {@link Obligation#overdueDates}: the scheduled dates from the
 * start date through yesterday without a resolution. An archived obligation has nothing left to resolve: no overdue
 * occurrences, no upcoming ones, and no next due date.
 *
 * @param resolvedDates the due dates of every resolution of the obligation (paid or skipped), including dates off
 *                      its current calendar, which are ignored
 */
public record ObligationTimeline(Recurrence recurrence, ObligationStatus status, Set<LocalDate> resolvedDates) {

    public ObligationTimeline {
        Objects.requireNonNull(recurrence, "Recurrence cannot be null");
        Objects.requireNonNull(status, "Obligation status cannot be null");
        resolvedDates = Set.copyOf(Objects.requireNonNull(resolvedDates, "Resolved dates cannot be null"));
    }

    /**
     * Lists the overdue occurrences in ascending order; empty when the obligation is archived.
     */
    public List<LocalDate> overdueDates(LocalDate today) {
        Objects.requireNonNull(today, "Today cannot be null");
        if (isArchived()) {
            return List.of();
        }
        OverdueRange range = OverdueRange.of(recurrence, today);
        return recurrence.unresolvedDatesBetween(range.from(), range.to(), resolvedDates);
    }

    /**
     * Counts the overdue occurrences in O(R), with R = resolved dates; zero when the obligation is archived.
     */
    public long overdueCount(LocalDate today) {
        Objects.requireNonNull(today, "Today cannot be null");
        if (isArchived()) {
            return 0;
        }
        OverdueRange range = OverdueRange.of(recurrence, today);
        return recurrence.countUnresolvedBetween(range.from(), range.to(), resolvedDates);
    }

    /**
     * Lists the first {@code limit} unresolved scheduled dates on or after {@code today}, in ascending order; empty
     * when the obligation is archived.
     */
    public List<LocalDate> upcomingDates(LocalDate today, int limit) {
        Objects.requireNonNull(today, "Today cannot be null");
        if (isArchived()) {
            return List.of();
        }
        return recurrence.firstDatesOnOrAfterExcluding(today, resolvedDates, limit);
    }

    /**
     * Returns the oldest unresolved occurrence: the oldest overdue date if any, otherwise the first unresolved
     * scheduled date on or after {@code today}. Empty when there is none or the obligation is archived.
     */
    public Optional<LocalDate> nextDueDate(LocalDate today) {
        Objects.requireNonNull(today, "Today cannot be null");
        if (isArchived()) {
            return Optional.empty();
        }
        OverdueRange range = OverdueRange.of(recurrence, today);
        return recurrence.firstDateBetweenExcluding(range.from(), range.to(), resolvedDates)
            .or(() -> recurrence.firstDatesOnOrAfterExcluding(today, resolvedDates, 1).stream().findFirst());
    }

    private boolean isArchived() {
        return status == ObligationStatus.ARCHIVED;
    }
}
