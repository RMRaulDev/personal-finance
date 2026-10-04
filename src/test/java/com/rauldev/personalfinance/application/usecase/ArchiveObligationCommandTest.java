package com.rauldev.personalfinance.application.usecase;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ArchiveObligationCommandTest {
    @Test
    void exposesAllFields() {
        UUID userId = UUID.randomUUID();
        UUID obligationId = UUID.randomUUID();

        ArchiveObligationCommand command = new ArchiveObligationCommand(userId, obligationId);

        assertEquals(userId, command.userId());
        assertEquals(obligationId, command.obligationId());
    }

    @Test
    void rejectsNullUserId() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ArchiveObligationCommand(null, UUID.randomUUID()));
        assertEquals("User id cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullObligationId() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ArchiveObligationCommand(UUID.randomUUID(), null));
        assertEquals("Obligation id cannot be null", e.getMessage());
    }
}
