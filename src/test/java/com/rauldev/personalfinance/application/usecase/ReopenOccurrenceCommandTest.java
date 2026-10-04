package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ReopenOccurrenceCommandTest {
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 8, 27);

    @Test
    void exposesAllFields() {
        UUID userId = UUID.randomUUID();
        UUID obligationId = UUID.randomUUID();

        ReopenOccurrenceCommand command = new ReopenOccurrenceCommand(userId, obligationId, DUE_DATE);

        assertEquals(userId, command.userId());
        assertEquals(obligationId, command.obligationId());
        assertEquals(DUE_DATE, command.dueDate());
    }

    @Test
    void rejectsNullUserId() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ReopenOccurrenceCommand(null, UUID.randomUUID(), DUE_DATE));
        assertEquals("User id cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullObligationId() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ReopenOccurrenceCommand(UUID.randomUUID(), null, DUE_DATE));
        assertEquals("Obligation id cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullDueDate() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ReopenOccurrenceCommand(UUID.randomUUID(), UUID.randomUUID(), null));
        assertEquals("Due date cannot be null", e.getMessage());
    }
}
