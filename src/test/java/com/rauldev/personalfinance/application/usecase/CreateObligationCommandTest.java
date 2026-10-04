package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Recurrence;

class CreateObligationCommandTest {
    private static final Recurrence RECURRENCE = new Recurrence(Frequency.MONTHLY, LocalDate.of(2026, 9, 1), null);

    @Test
    void exposesAllFields() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        CreateObligationCommand command = new CreateObligationCommand(userId, accountId, categoryId, "Rent",
            Money.ofCents(100), RECURRENCE);

        assertEquals(userId, command.userId());
        assertEquals(accountId, command.accountId());
        assertEquals(categoryId, command.categoryId());
        assertEquals("Rent", command.name());
        assertEquals(Money.ofCents(100), command.amount());
        assertEquals(RECURRENCE, command.recurrence());
    }

    @Test
    void rejectsNullUserId() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new CreateObligationCommand(
            null, UUID.randomUUID(), UUID.randomUUID(), "Rent", Money.ofCents(100), RECURRENCE));
        assertEquals("User id cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullAccountId() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new CreateObligationCommand(
            UUID.randomUUID(), null, UUID.randomUUID(), "Rent", Money.ofCents(100), RECURRENCE));
        assertEquals("Account id cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullCategoryId() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new CreateObligationCommand(
            UUID.randomUUID(), UUID.randomUUID(), null, "Rent", Money.ofCents(100), RECURRENCE));
        assertEquals("Category id cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullName() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new CreateObligationCommand(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, Money.ofCents(100), RECURRENCE));
        assertEquals("Obligation name cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullAmount() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new CreateObligationCommand(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Rent", null, RECURRENCE));
        assertEquals("Amount cannot be null", e.getMessage());
    }

    @Test
    void rejectsNullRecurrence() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new CreateObligationCommand(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Rent", Money.ofCents(100), null));
        assertEquals("Recurrence cannot be null", e.getMessage());
    }
}
