package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Value Object describing the calendar of an obligation: its frequency, its anchor ({@code startDate}) and its
 * optional inclusive {@code endDate}.
 *
 * Date {@code n} (n = 0, 1, 2, ...) is always computed from the anchor, never from the previous date, so month-end
 * and leap-year clamping never accumulates. Every operation is pure: {@code countBetween} and {@code isScheduled}
 * are O(1), {@code countUnresolvedBetween} is O(R) with R = resolved dates, {@code datesBetween} is O(k) with
 * k = returned dates, and {@code unresolvedDatesBetween} is O(k) with k = scheduled dates in the range, because it
 * enumerates them all before filtering.
 */
public final class Recurrence {
    private static final long WEEKLY_STEP_DAYS = 7;
    private static final long BIWEEKLY_STEP_DAYS = 14;

    private final Frequency frequency;
    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * @param endDate the last possible due date (inclusive), or {@code null} if the calendar has no end
     */
    public Recurrence(Frequency frequency, LocalDate startDate, LocalDate endDate) {
        this.frequency = Objects.requireNonNull(frequency, "Frequency cannot be null");
        this.startDate = Objects.requireNonNull(startDate, "Start date cannot be null");
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        this.endDate = endDate;
    }

    public Frequency frequency() {
        return frequency;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public Optional<LocalDate> endDate() {
        return Optional.ofNullable(endDate);
    }

    /**
     * Counts the scheduled dates within {@code [from, to]} (both inclusive) in O(1). An empty range counts zero.
     */
    public long countBetween(LocalDate from, LocalDate to) {
        return indexRange(from, to)
            .map(range -> range.last() - range.first() + 1)
            .orElse(0L);
    }

    /**
     * Lists the scheduled dates within {@code [from, to]} (both inclusive), in ascending order.
     */
    public List<LocalDate> datesBetween(LocalDate from, LocalDate to) {
        Optional<IndexRange> range = indexRange(from, to);
        if (range.isEmpty()) {
            return List.of();
        }
        List<LocalDate> dates = new ArrayList<>();
        for (long index = range.get().first(); index <= range.get().last(); index++) {
            dates.add(dateAt(index));
        }
        return List.copyOf(dates);
    }

    /**
     * Counts the scheduled dates within {@code [from, to]} that are not in {@code resolvedDates}. Resolved dates
     * outside the range or not on this calendar are ignored. O(R), with R = resolved dates.
     */
    public long countUnresolvedBetween(LocalDate from, LocalDate to, Set<LocalDate> resolvedDates) {
        Objects.requireNonNull(resolvedDates, "Resolved dates cannot be null");
        long scheduled = countBetween(from, to);
        long resolved = resolvedDates.stream()
            .filter(date -> !date.isBefore(from) && !date.isAfter(to) && isScheduled(date))
            .count();
        return scheduled - resolved;
    }

    /**
     * Lists the scheduled dates within {@code [from, to]} (both inclusive) that are not in {@code resolvedDates},
     * in ascending order.
     */
    public List<LocalDate> unresolvedDatesBetween(LocalDate from, LocalDate to, Set<LocalDate> resolvedDates) {
        Objects.requireNonNull(resolvedDates, "Resolved dates cannot be null");
        return datesBetween(from, to).stream()
            .filter(date -> !resolvedDates.contains(date))
            .toList();
    }

    /**
     * Returns the earliest scheduled date within {@code [from, to]} that is not in {@code excluded}. It visits at
     * most {@code excluded.size() + 1} dates.
     */
    public Optional<LocalDate> firstDateBetweenExcluding(LocalDate from, LocalDate to, Set<LocalDate> excluded) {
        Objects.requireNonNull(excluded, "Excluded dates cannot be null");
        Optional<IndexRange> range = indexRange(from, to);
        if (range.isEmpty()) {
            return Optional.empty();
        }
        for (long index = range.get().first(); index <= range.get().last(); index++) {
            LocalDate date = dateAt(index);
            if (!excluded.contains(date)) {
                return Optional.of(date);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks in O(1) whether the date belongs to this calendar.
     */
    public boolean isScheduled(LocalDate date) {
        Objects.requireNonNull(date, "Date cannot be null");
        if (date.isBefore(startDate) || (endDate != null && date.isAfter(endDate))) {
            return false;
        }
        return dateAt(lastIndexOnOrBefore(date)).equals(date);
    }

    private Optional<IndexRange> indexRange(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "From date cannot be null");
        Objects.requireNonNull(to, "To date cannot be null");
        LocalDate lower = from.isAfter(startDate) ? from : startDate;
        LocalDate upper = endDate != null && endDate.isBefore(to) ? endDate : to;
        if (lower.isAfter(upper)) {
            return Optional.empty();
        }
        long first = lastIndexOnOrBefore(lower);
        if (dateAt(first).isBefore(lower)) {
            first++;
        }
        long last = lastIndexOnOrBefore(upper);
        if (first > last) {
            return Optional.empty();
        }
        return Optional.of(new IndexRange(first, last));
    }

    /**
     * Largest index whose date is on or before {@code date}. Requires {@code date >= startDate}.
     */
    private long lastIndexOnOrBefore(LocalDate date) {
        return switch (frequency) {
            case ONCE -> 0;
            case WEEKLY -> ChronoUnit.DAYS.between(startDate, date) / WEEKLY_STEP_DAYS;
            case BIWEEKLY -> ChronoUnit.DAYS.between(startDate, date) / BIWEEKLY_STEP_DAYS;
            case MONTHLY -> adjustToLastIndexOnOrBefore(ChronoUnit.MONTHS.between(startDate, date), date);
            case YEARLY -> adjustToLastIndexOnOrBefore(ChronoUnit.YEARS.between(startDate, date), date);
        };
    }

    /**
     * Corrects a month/year estimate by at most a few steps, because clamping to the last valid day of a month can
     * put the date of the estimated index on either side of {@code date}.
     */
    private long adjustToLastIndexOnOrBefore(long estimate, LocalDate date) {
        long index = estimate;
        while (index > 0 && dateAt(index).isAfter(date)) {
            index--;
        }
        while (!dateAt(index + 1).isAfter(date)) {
            index++;
        }
        return index;
    }

    private LocalDate dateAt(long index) {
        return switch (frequency) {
            case ONCE -> startDate;
            case WEEKLY -> startDate.plusDays(Math.multiplyExact(index, WEEKLY_STEP_DAYS));
            case BIWEEKLY -> startDate.plusDays(Math.multiplyExact(index, BIWEEKLY_STEP_DAYS));
            case MONTHLY -> startDate.plusMonths(index);
            case YEARLY -> startDate.plusYears(index);
        };
    }

    private record IndexRange(long first, long last) {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Recurrence recurrence)) {
            return false;
        }
        return frequency == recurrence.frequency
            && startDate.equals(recurrence.startDate)
            && Objects.equals(endDate, recurrence.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frequency, startDate, endDate);
    }

    @Override
    public String toString() {
        return "Recurrence[frequency=" + frequency + ", startDate=" + startDate + ", endDate=" + endDate + "]";
    }
}
