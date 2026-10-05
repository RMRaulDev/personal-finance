package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Body of {@code POST /api/v1/obligations/{obligationId}/overdue-occurrences/skip}: the due dates
 * that were skipped, in ascending order (empty when nothing was overdue).
 */
public record SkippedOccurrencesResponse(List<LocalDate> skippedDueDates) {

    public SkippedOccurrencesResponse {
        skippedDueDates = List.copyOf(Objects.requireNonNull(skippedDueDates, "Skipped due dates cannot be null"));
    }
}
