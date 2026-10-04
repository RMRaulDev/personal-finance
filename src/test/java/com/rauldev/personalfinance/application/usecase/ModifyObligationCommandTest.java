package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Recurrence;

class ModifyObligationCommandTest {
    private static final Recurrence RECURRENCE = new Recurrence(Frequency.MONTHLY, LocalDate.of(2026, 9, 1), null);

    @Test
    void exposesAllFields() {
        UUID userId = UUID.randomUUID();
        UUID obligationId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        ModifyObligationCommand command = new ModifyObligationCommand(userId, obligationId, "Rent",
            Money.ofCents(100), accountId, categoryId, RECURRENCE);

        assertEquals(userId, command.userId());
        assertEquals(obligationId, command.obligationId());
        assertEquals("Rent", command.name());
        assertEquals(Money.ofCents(100), command.amount());
        assertEquals(accountId, command.accountId());
        assertEquals(categoryId, command.categoryId());
        assertEquals(RECURRENCE, command.recurrence());
    }

    @Test
    void rejectsNullUserId() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ModifyObligationCommand(null, UUID.randomUUID(), "Rent", null, null, null, null));
        assertEquals("User id cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullObligationId() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ModifyObligationCommand(UUID.randomUUID(), null, "Rent", null, null, null, null));
        assertEquals("Obligation id cannot be null", e.getMessage());
    }

    @Test
    void rejectsCommandWithoutAnyChange() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new ModifyObligationCommand(UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null));
        assertEquals("At least one change is required", e.getMessage());
    }

    @Test
    void acceptsEachSingleChange() {
        UUID userId = UUID.randomUUID();
        UUID obligationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        assertEquals("Rent", new ModifyObligationCommand(userId, obligationId, "Rent", null, null, null, null).name());
        assertEquals(Money.ofCents(1),
            new ModifyObligationCommand(userId, obligationId, null, Money.ofCents(1), null, null, null).amount());
        assertEquals(accountId,
            new ModifyObligationCommand(userId, obligationId, null, null, accountId, null, null).accountId());
        assertEquals(categoryId,
            new ModifyObligationCommand(userId, obligationId, null, null, null, categoryId, null).categoryId());
        assertEquals(RECURRENCE,
            new ModifyObligationCommand(userId, obligationId, null, null, null, null, RECURRENCE).recurrence());
    }
}
