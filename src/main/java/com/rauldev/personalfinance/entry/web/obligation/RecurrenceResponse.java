package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;
import java.util.Objects;

import com.rauldev.personalfinance.domain.Recurrence;

/**
 * Calendar of an obligation inside a response body, with the same shape as {@link RecurrenceRequest}:
 * {@code {"frequency": "MONTHLY", "startDate": "2026-10-04", "endDate": null}}. {@code endDate} is
 * {@code null} (serialized, not omitted) when the calendar has no end.
 */
public record RecurrenceResponse(String frequency, LocalDate startDate, LocalDate endDate) {

    public static RecurrenceResponse from(Recurrence recurrence) {
        Objects.requireNonNull(recurrence, "Recurrence cannot be null");
        return new RecurrenceResponse(
            recurrence.frequency().name(), recurrence.startDate(), recurrence.endDate().orElse(null));
    }
}
