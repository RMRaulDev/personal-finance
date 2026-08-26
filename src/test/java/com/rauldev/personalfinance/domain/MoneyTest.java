package com.rauldev.personalfinance.domain;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Money Value Object")
class MoneyTest {

    @Test
    @DisplayName("should create Money with valid positive amount")
    void createMoneyWithValidPositiveAmount() {
        Money money = Money.of(new BigDecimal("100.50"));
        assertNotNull(money);
        assertEquals(new BigDecimal("100.50"), money.amount());
    }

    @Test
    @DisplayName("should create Money with zero amount")
    void createMoneyWithZero() {
        Money money = Money.of(BigDecimal.ZERO);
        assertNotNull(money);
        assertEquals(BigDecimal.ZERO.setScale(2), money.amount());
        assertTrue(money.isZero());
    }

    @Test
    @DisplayName("should throw exception when creating Money with negative amount")
    void createMoneyWithNegativeAmountThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Money.of(new BigDecimal("-10.00")),
            "Money amount cannot be negative"
        );
    }

    @Test
    @DisplayName("should throw exception when amount is null")
    void createMoneyWithNullAmountThrows() {
        assertThrows(NullPointerException.class, () -> Money.of((BigDecimal) null));
    }

    @Test
    @DisplayName("should create Money from cents")
    void createMoneyFromCents() {
        Money money = Money.ofCents(1000);
        assertEquals(new BigDecimal("10.00"), money.amount());
    }

    @Test
    @DisplayName("should create Money from cents with zero")
    void createMoneyFromCentsWithZero() {
        Money money = Money.ofCents(0);
        assertTrue(money.isZero());
    }

    @Test
    @DisplayName("should throw exception when creating Money from negative cents")
    void createMoneyFromNegativeCentsThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Money.ofCents(-100),
            "Money amount cannot be negative"
        );
    }

    @Test
    @DisplayName("should create Money from valid string amount")
    void createMoneyFromString() {
        Money money = Money.of("50.75");
        assertEquals(new BigDecimal("50.75"), money.amount());
    }

    @Test
    @DisplayName("should throw exception when creating Money from invalid string")
    void createMoneyFromInvalidStringThrows() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("not a number"));
    }

    @Test
    @DisplayName("should throw exception when creating Money from negative string")
    void createMoneyFromNegativeStringThrows() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("-25.00"));
    }

    @Test
    @DisplayName("should have value-based equality")
    void moneyEqualityBasedOnValue() {
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = Money.of(new BigDecimal("100.00"));
        Money money3 = Money.of(new BigDecimal("50.00"));

        assertEquals(money1, money2);
        assertNotEquals(money1, money3);
    }

    @Test
    @DisplayName("should be equal to itself")
    void moneyEqualityToSelf() {
        Money money = Money.of(new BigDecimal("100.00"));
        assertEquals(money, money);
    }

    @Test
    @DisplayName("should have consistent hashCode for equal Money instances")
    void moneyHashCodeConsistent() {
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = Money.of(new BigDecimal("100.00"));

        assertEquals(money1.hashCode(), money2.hashCode());
    }

    @Test
    @DisplayName("should have different hashCode for different Money instances")
    void moneyHashCodeDifferent() {
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = Money.of(new BigDecimal("50.00"));

        assertNotEquals(money1.hashCode(), money2.hashCode());
    }

    @Test
    @DisplayName("should add Money amounts correctly")
    void addMoney() {
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = Money.of(new BigDecimal("50.50"));

        Money result = money1.add(money2);

        assertEquals(new BigDecimal("150.50"), result.amount());
    }

    @Test
    @DisplayName("should add Money resulting in zero")
    void addMoneyResultingInZero() {
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = Money.of(new BigDecimal("0.00"));

        Money result = money1.add(money2);

        assertEquals(money1, result);
    }

    @Test
    @DisplayName("should throw exception when adding null Money")
    void addNullMoneyThrows() {
        Money money = Money.of(new BigDecimal("100.00"));
        assertThrows(NullPointerException.class, () -> money.add(null));
    }

    @Test
    @DisplayName("should subtract Money amounts correctly")
    void subtractMoney() {
        Money money1 = Money.of(new BigDecimal("150.00"));
        Money money2 = Money.of(new BigDecimal("50.50"));

        Money result = money1.subtract(money2);

        assertEquals(new BigDecimal("99.50"), result.amount());
    }

    @Test
    @DisplayName("should subtract Money resulting in zero")
    void subtractMoneyResultingInZero() {
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = Money.of(new BigDecimal("100.00"));

        Money result = money1.subtract(money2);

        assertTrue(result.isZero());
    }

    @Test
    @DisplayName("should throw exception when subtracting results in negative")
    void subtractMoneyResultingInNegativeThrows() {
        Money money1 = Money.of(new BigDecimal("50.00"));
        Money money2 = Money.of(new BigDecimal("100.00"));

        assertThrows(
            IllegalArgumentException.class,
            () -> money1.subtract(money2),
            "Money amount cannot be negative"
        );
    }

    @Test
    @DisplayName("should throw exception when subtracting null Money")
    void subtractNullMoneyThrows() {
        Money money = Money.of(new BigDecimal("100.00"));
        assertThrows(NullPointerException.class, () -> money.subtract(null));
    }

    @Test
    @DisplayName("should identify zero Money")
    void isZero() {
        Money zero = Money.of(BigDecimal.ZERO);
        Money nonZero = Money.of(new BigDecimal("0.01"));

        assertTrue(zero.isZero());
        assertFalse(nonZero.isZero());
    }

    @Test
    @DisplayName("should identify positive Money")
    void isPositive() {
        Money positive = Money.of(new BigDecimal("0.01"));
        Money zero = Money.of(BigDecimal.ZERO);

        assertTrue(positive.isPositive());
        assertFalse(zero.isPositive());
    }

    @Test
    @DisplayName("should compare Money amounts correctly")
    void compareToMoney() {
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = Money.of(new BigDecimal("100.00"));
        Money money3 = Money.of(new BigDecimal("50.00"));
        Money money4 = Money.of(new BigDecimal("150.00"));

        assertEquals(0, money1.compareTo(money2));
        assertTrue(money3.compareTo(money1) < 0);
        assertTrue(money4.compareTo(money1) > 0);
    }

    @Test
    @DisplayName("should throw exception when comparing with null Money")
    void compareToNullMoneyThrows() {
        Money money = Money.of(new BigDecimal("100.00"));
        assertThrows(NullPointerException.class, () -> money.compareTo(null));
    }

    @Test
    @DisplayName("should be immutable - amount cannot change")
    void immutability() {
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = money1.add(Money.of(new BigDecimal("50.00")));

        assertEquals(new BigDecimal("100.00"), money1.amount());
        assertEquals(new BigDecimal("150.00"), money2.amount());
    }

    @Test
    @DisplayName("should normalize scale to 2 decimal places")
    void normalizeScale() {
        Money money = Money.of(new BigDecimal("100"));
        assertEquals(2, money.amount().scale());
        assertEquals(new BigDecimal("100.00"), money.amount());
    }

    @Test
    @DisplayName("should have proper string representation")
    void stringRepresentation() {
        Money money = Money.of(new BigDecimal("100.50"));
        assertEquals("100.50", money.toString());
    }

    @Test
    @DisplayName("should reject equal Money in not-equal comparison")
    void notEqualsDifferentType() {
        Money money = Money.of(new BigDecimal("100.00"));
        assertNotEquals(money, "100.00");
        assertNotEquals(money, null);
    }
}
