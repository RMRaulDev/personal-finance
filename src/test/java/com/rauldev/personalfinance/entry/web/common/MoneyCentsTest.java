package com.rauldev.personalfinance.entry.web.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.domain.Money;

class MoneyCentsTest {

    @Test
    void convertsCentsToMoney() {
        assertEquals(Money.of("10.05"), MoneyCents.toMoney(1005L, "amountCents"));
    }

    @Test
    void convertsZeroCentsToZeroMoney() {
        assertEquals(Money.ofCents(0), MoneyCents.toMoney(0L, "amountCents"));
    }

    @Test
    void rejectsMissingCentsNamingTheField() {
        IllegalArgumentException ex =
            assertThrows(IllegalArgumentException.class, () -> MoneyCents.toMoney(null, "amountCents"));

        assertEquals("Field 'amountCents' is required", ex.getMessage());
    }

    @Test
    void rejectsNegativeCents() {
        IllegalArgumentException ex =
            assertThrows(IllegalArgumentException.class, () -> MoneyCents.toMoney(-1L, "amountCents"));

        assertEquals("Money amount cannot be negative", ex.getMessage());
    }

    @Test
    void convertsMoneyToCents() {
        assertEquals(1005L, MoneyCents.toCents(Money.of("10.05")));
    }

    @Test
    void convertsZeroMoneyToZeroCents() {
        assertEquals(0L, MoneyCents.toCents(Money.ofCents(0)));
    }

    @Test
    void roundTripsLargeAmounts() {
        assertEquals(Long.MAX_VALUE, MoneyCents.toCents(MoneyCents.toMoney(Long.MAX_VALUE, "amountCents")));
    }

    @Test
    void rejectsNullMoney() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> MoneyCents.toCents(null));

        assertTrue(ex.getMessage().contains("Money cannot be null"));
    }

    @Test
    void toCentsRejectsAmountsBeyondLongRange() {
        Money beyondLong = Money.ofCents(Long.MAX_VALUE).add(Money.ofCents(1));

        assertThrows(ArithmeticException.class, () -> MoneyCents.toCents(beyondLong));
    }
}
