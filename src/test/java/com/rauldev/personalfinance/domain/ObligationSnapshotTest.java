package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ObligationSnapshotTest {
    private static final Money AMOUNT = Money.ofCents(100);
    private static final Recurrence RECURRENCE = new Recurrence(Frequency.ONCE, LocalDate.of(2026, 8, 20), null);

    private static String message(UUID id, String name, Money amount, UUID accountId, UUID categoryId,
                                  Recurrence recurrence, ObligationStatus status) {
        return assertThrows(NullPointerException.class,
            () -> new ObligationSnapshot(id, name, amount, accountId, categoryId, recurrence, status)).getMessage();
    }

    @Test
    void rejectsNullFieldsWithTheirMessages() {
        UUID id = UUID.randomUUID();
        ObligationStatus active = ObligationStatus.ACTIVE;

        assertEquals("Obligation id cannot be null", message(null, "A", AMOUNT, id, id, RECURRENCE, active));
        assertEquals("Obligation name cannot be null", message(id, null, AMOUNT, id, id, RECURRENCE, active));
        assertEquals("Amount cannot be null", message(id, "A", null, id, id, RECURRENCE, active));
        assertEquals("Account id cannot be null", message(id, "A", AMOUNT, null, id, RECURRENCE, active));
        assertEquals("Category id cannot be null", message(id, "A", AMOUNT, id, null, RECURRENCE, active));
        assertEquals("Recurrence cannot be null", message(id, "A", AMOUNT, id, id, null, active));
        assertEquals("Obligation status cannot be null", message(id, "A", AMOUNT, id, id, RECURRENCE, null));
    }

    @Test
    void rejectsZeroAmount() {
        UUID id = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new ObligationSnapshot(id, "A", Money.ofCents(0), id, id, RECURRENCE, ObligationStatus.ACTIVE));

        assertEquals("Obligation amount must be greater than zero", exception.getMessage());
    }
}
