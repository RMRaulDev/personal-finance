package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;

import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.entry.web.common.RequiredFields;

/**
 * Calendar of an obligation inside a request body: {@code {"frequency": "MONTHLY", "startDate":
 * "2026-10-04", "endDate": null}}. {@code frequency} is a {@link Frequency} constant name
 * ({@code ONCE}, {@code WEEKLY}, {@code BIWEEKLY}, {@code MONTHLY}, {@code YEARLY}; an unknown value
 * answers 400 while the JSON body is read). {@code frequency} and {@code startDate} are required;
 * {@code endDate} is optional, and a missing or {@code null} {@code endDate} means the calendar has
 * no end.
 */
public record RecurrenceRequest(Frequency frequency, LocalDate startDate, LocalDate endDate) {

    /**
     * @throws IllegalArgumentException if {@code frequency} or {@code startDate} is missing, or
     *     {@code endDate} is before {@code startDate}
     */
    public Recurrence toRecurrence() {
        return new Recurrence(
            RequiredFields.require(frequency, "recurrence.frequency"),
            RequiredFields.require(startDate, "recurrence.startDate"),
            endDate);
    }
}
