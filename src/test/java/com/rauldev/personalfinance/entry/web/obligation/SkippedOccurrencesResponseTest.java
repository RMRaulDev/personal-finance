package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class SkippedOccurrencesResponseTest {

    @Test
    void keepsTheDatesInOrder() {
        List<LocalDate> dates = List.of(LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));

        assertEquals(dates, new SkippedOccurrencesResponse(dates).skippedDueDates());
    }

    @Test
    void copiesTheListDefensively() {
        List<LocalDate> dates = new ArrayList<>(List.of(LocalDate.of(2026, 8, 20)));
        SkippedOccurrencesResponse response = new SkippedOccurrencesResponse(dates);

        dates.add(LocalDate.of(2026, 9, 20));

        assertEquals(List.of(LocalDate.of(2026, 8, 20)), response.skippedDueDates());
    }

    @Test
    void exposesAnImmutableList() {
        SkippedOccurrencesResponse response = new SkippedOccurrencesResponse(List.of(LocalDate.of(2026, 8, 20)));

        assertThrows(UnsupportedOperationException.class,
            () -> response.skippedDueDates().add(LocalDate.of(2026, 9, 20)));
    }

    @Test
    void rejectsNullDates() {
        assertThrows(NullPointerException.class, () -> new SkippedOccurrencesResponse(null));
    }
}
