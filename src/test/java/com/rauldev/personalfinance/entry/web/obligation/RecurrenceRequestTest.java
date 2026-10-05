package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Recurrence;

class RecurrenceRequestTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 15);
    private static final LocalDate END = LocalDate.of(2026, 12, 15);

    @Test
    void mapsAllFieldsToRecurrence() {
        Recurrence recurrence = new RecurrenceRequest(Frequency.MONTHLY, START, END).toRecurrence();

        assertEquals(Frequency.MONTHLY, recurrence.frequency());
        assertEquals(START, recurrence.startDate());
        assertEquals(END, recurrence.endDate().orElseThrow());
    }

    @Test
    void acceptsMissingEndDate() {
        Recurrence recurrence = new RecurrenceRequest(Frequency.WEEKLY, START, null).toRecurrence();

        assertTrue(recurrence.endDate().isEmpty());
    }

    @Test
    void rejectsMissingFrequencyNamingItWithTheDottedName() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new RecurrenceRequest(null, START, null).toRecurrence());

        assertEquals("Field 'recurrence.frequency' is required", e.getMessage());
    }

    @Test
    void rejectsMissingStartDateNamingItWithTheDottedName() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new RecurrenceRequest(Frequency.ONCE, null, null).toRecurrence());

        assertEquals("Field 'recurrence.startDate' is required", e.getMessage());
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThrows(IllegalArgumentException.class,
            () -> new RecurrenceRequest(Frequency.ONCE, START, START.minusDays(1)).toRecurrence());
    }
}
