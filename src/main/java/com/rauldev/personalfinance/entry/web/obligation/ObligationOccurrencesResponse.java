package com.rauldev.personalfinance.entry.web.obligation;

import java.util.List;
import java.util.Objects;

import com.rauldev.personalfinance.application.readmodel.ObligationOccurrences;

/**
 * Body of {@code GET /api/v1/obligations/{obligationId}/occurrences}:
 * {@code {overdue, upcoming, resolved}}, each a list of {@link OccurrenceResponse} ({@code []} when
 * empty). {@code overdue} holds every overdue occurrence and {@code upcoming} the next 3 unresolved
 * ones, both ascending by due date; {@code resolved} holds the 4 most recent resolutions, descending
 * by due date.
 */
public record ObligationOccurrencesResponse(
    List<OccurrenceResponse> overdue,
    List<OccurrenceResponse> upcoming,
    List<OccurrenceResponse> resolved
) {

    public static ObligationOccurrencesResponse from(ObligationOccurrences occurrences) {
        Objects.requireNonNull(occurrences, "Obligation occurrences cannot be null");
        return new ObligationOccurrencesResponse(
            occurrences.overdue().stream().map(OccurrenceResponse::from).toList(),
            occurrences.upcoming().stream().map(OccurrenceResponse::from).toList(),
            occurrences.resolved().stream().map(OccurrenceResponse::from).toList());
    }
}
