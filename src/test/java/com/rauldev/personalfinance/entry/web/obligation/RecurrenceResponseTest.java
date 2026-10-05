package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Recurrence;

class RecurrenceResponseTest {

    @Test
    void mapsFrequencyNameAndDates() {
        Recurrence recurrence = new Recurrence(Frequency.BIWEEKLY, LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 12, 1));

        assertEquals(new RecurrenceResponse("BIWEEKLY", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 1)),
            RecurrenceResponse.from(recurrence));
    }

    @Test
    void mapsAnOpenEndedCalendarToNullEndDate() {
        Recurrence recurrence = new Recurrence(Frequency.ONCE, LocalDate.of(2026, 10, 1), null);

        RecurrenceResponse response = RecurrenceResponse.from(recurrence);

        assertEquals("ONCE", response.frequency());
        assertNull(response.endDate());
    }

    @Test
    void mapsEveryFrequencyToItsName() {
        for (Frequency frequency : Frequency.values()) {
            RecurrenceResponse response =
                RecurrenceResponse.from(new Recurrence(frequency, LocalDate.of(2026, 10, 1), null));

            assertEquals(frequency.name(), response.frequency());
        }
    }

    @Test
    void rejectsNullRecurrence() {
        assertThrows(NullPointerException.class, () -> RecurrenceResponse.from(null));
    }
}
