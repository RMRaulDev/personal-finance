package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AttentionTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 20);
    private static final Money MONEY = Money.ofCents(100);

    @Test
    void eachVariantReportsItsType() {
        assertEquals(AttentionType.OVERDUE_OCCURRENCE,
            new Attention.OverdueOccurrence(UUID.randomUUID(), DATE, 1, MONEY).type());
        assertEquals(AttentionType.SHORTFALL, new Attention.Shortfall(MONEY).type());
        assertEquals(AttentionType.PAYMENT_BLOCKED,
            new Attention.PaymentBlocked(UUID.randomUUID(), DATE, MONEY).type());
        assertEquals(AttentionType.ACCOUNT_SHORTFALL,
            new Attention.AccountShortfall(UUID.randomUUID(), MONEY, DATE).type());
    }

    @Test
    void typesAreDeclaredInPriorityOrder() {
        assertEquals(List.of(AttentionType.OVERDUE_OCCURRENCE, AttentionType.SHORTFALL,
            AttentionType.PAYMENT_BLOCKED, AttentionType.ACCOUNT_SHORTFALL), List.of(AttentionType.values()));
    }

    @Test
    void overdueOccurrenceRejectsNonPositiveCount() {
        UUID id = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Attention.OverdueOccurrence(id, DATE, 0, MONEY));

        assertEquals("Overdue count must be greater than zero", exception.getMessage());
    }

    @Test
    void overdueOccurrenceRejectsNullFields() {
        UUID id = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> new Attention.OverdueOccurrence(null, DATE, 1, MONEY));
        assertThrows(NullPointerException.class, () -> new Attention.OverdueOccurrence(id, null, 1, MONEY));
        assertThrows(NullPointerException.class, () -> new Attention.OverdueOccurrence(id, DATE, 1, null));
    }

    @Test
    void shortfallRejectsNullAmount() {
        assertThrows(NullPointerException.class, () -> new Attention.Shortfall(null));
    }

    @Test
    void paymentBlockedRejectsNullFields() {
        UUID id = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> new Attention.PaymentBlocked(null, DATE, MONEY));
        assertThrows(NullPointerException.class, () -> new Attention.PaymentBlocked(id, null, MONEY));
        assertThrows(NullPointerException.class, () -> new Attention.PaymentBlocked(id, DATE, null));
    }

    @Test
    void accountShortfallRejectsNullFields() {
        UUID id = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> new Attention.AccountShortfall(null, MONEY, DATE));
        assertThrows(NullPointerException.class, () -> new Attention.AccountShortfall(id, null, DATE));
        assertThrows(NullPointerException.class, () -> new Attention.AccountShortfall(id, MONEY, null));
    }
}
