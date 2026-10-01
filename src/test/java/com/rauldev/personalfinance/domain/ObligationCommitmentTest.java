package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ObligationCommitmentTest {
    private static final Money ZERO = Money.ofCents(0);

    @Test
    void acceptsZeroOverdueCount() {
        ObligationCommitment commitment = new ObligationCommitment(UUID.randomUUID(), ZERO, 0, ZERO, List.of(), false);

        assertEquals(0, commitment.overdueCount());
    }

    @Test
    void rejectsNegativeOverdueCount() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new ObligationCommitment(UUID.randomUUID(), ZERO, -1, ZERO, List.of(), false));

        assertEquals("Overdue count cannot be negative", exception.getMessage());
    }

    @Test
    void rejectsNullFields() {
        UUID id = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> new ObligationCommitment(null, ZERO, 0, ZERO, List.of(), false));
        assertThrows(NullPointerException.class, () -> new ObligationCommitment(id, null, 0, ZERO, List.of(), false));
        assertThrows(NullPointerException.class, () -> new ObligationCommitment(id, ZERO, 0, null, List.of(), false));
        assertThrows(NullPointerException.class, () -> new ObligationCommitment(id, ZERO, 0, ZERO, null, false));
    }

    @Test
    void copiesPendingDatesDefensively() {
        List<LocalDate> dates = new ArrayList<>(List.of(LocalDate.of(2026, 8, 20)));

        ObligationCommitment commitment = new ObligationCommitment(UUID.randomUUID(), ZERO, 0, ZERO, dates, false);
        dates.clear();

        assertEquals(List.of(LocalDate.of(2026, 8, 20)), commitment.pendingDates());
        assertThrows(UnsupportedOperationException.class, () -> commitment.pendingDates().clear());
    }
}
