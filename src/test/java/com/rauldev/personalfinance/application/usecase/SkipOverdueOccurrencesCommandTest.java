package com.rauldev.personalfinance.application.usecase;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class SkipOverdueOccurrencesCommandTest {
    @Test
    void exposesAllFields() {
        UUID userId = UUID.randomUUID();
        UUID obligationId = UUID.randomUUID();

        SkipOverdueOccurrencesCommand command = new SkipOverdueOccurrencesCommand(userId, obligationId);

        assertEquals(userId, command.userId());
        assertEquals(obligationId, command.obligationId());
    }

    @Test
    void rejectsNullUserId() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new SkipOverdueOccurrencesCommand(null, UUID.randomUUID()));
        assertEquals("User id cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullObligationId() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new SkipOverdueOccurrencesCommand(UUID.randomUUID(), null));
        assertEquals("Obligation id cannot be null", e.getMessage());
    }
}
