package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.domain.Money;

class PayOccurrenceCommandTest {
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 8, 20);

    private final UUID userId = UUID.randomUUID();
    private final UUID obligationId = UUID.randomUUID();

    @Test
    void exposesAllFields() {
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate operationDate = LocalDate.of(2026, 8, 18);

        PayOccurrenceCommand command = new PayOccurrenceCommand(userId, obligationId, DUE_DATE,
            Money.ofCents(1234), operationDate, accountId, categoryId);

        assertEquals(userId, command.userId());
        assertEquals(obligationId, command.obligationId());
        assertEquals(DUE_DATE, command.dueDate());
        assertEquals(Money.ofCents(1234), command.amount());
        assertEquals(operationDate, command.operationDate());
        assertEquals(accountId, command.accountId());
        assertEquals(categoryId, command.categoryId());
    }

    @Test
    void optionalFieldsMayBeNull() {
        PayOccurrenceCommand command = new PayOccurrenceCommand(userId, obligationId, DUE_DATE,
            null, null, null, null);

        assertNull(command.amount());
        assertNull(command.operationDate());
        assertNull(command.accountId());
        assertNull(command.categoryId());
    }

    @Test
    void rejectsNullUserId() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new PayOccurrenceCommand(null, obligationId, DUE_DATE, null, null, null, null));
        assertEquals("User id cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullObligationId() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new PayOccurrenceCommand(userId, null, DUE_DATE, null, null, null, null));
        assertEquals("Obligation id cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullDueDate() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new PayOccurrenceCommand(userId, obligationId, null, null, null, null, null));
        assertEquals("Due date cannot be null", e.getMessage());
    }

    @Test
    void rejectsZeroAmount() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new PayOccurrenceCommand(userId, obligationId, DUE_DATE, Money.ofCents(0), null, null, null));
        assertEquals("Amount must be greater than zero", e.getMessage());
    }

    @Test
    void acceptsSmallestPositiveAmount() {
        PayOccurrenceCommand command = new PayOccurrenceCommand(userId, obligationId, DUE_DATE,
            Money.ofCents(1), null, null, null);

        assertEquals(Money.ofCents(1), command.amount());
    }
}
