package com.rauldev.personalfinance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Money is a Value Object that represents a monetary amount.
 *
 * Invariants:
 * - The amount is never negative.
 * - The amount is always stored with a scale of 2 (standard for currency).
 *
 * This class is immutable and uses value-based equality.
 *
 * @author Raul
 */
public final class Money {
    private static final int CURRENCY_SCALE = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(CURRENCY_SCALE);
    private static final String NEGATIVE_AMOUNT_ERROR = "Money amount cannot be negative";

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(CURRENCY_SCALE, RoundingMode.UNNECESSARY);
    }

    /**
     * Creates a Money instance with the given amount.
     *
     * @param amount the monetary amount as a BigDecimal
     * @return a new Money instance
     * @throws IllegalArgumentException if the amount is negative
     * @throws NullPointerException if the amount is null
     */
    public static Money of(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException(NEGATIVE_AMOUNT_ERROR);
        }
        return new Money(amount);
    }

    /**
     * Creates a Money instance with the given amount as a long value.
     *
     * The amount is interpreted as the number of cents (e.g., 1000 = 10.00).
     *
     * @param centAmount the monetary amount in cents
     * @return a new Money instance
     * @throws IllegalArgumentException if the amount is negative
     */
    public static Money ofCents(long centAmount) {
        if (centAmount < 0) {
            throw new IllegalArgumentException(NEGATIVE_AMOUNT_ERROR);
        }
        return new Money(BigDecimal.valueOf(centAmount, CURRENCY_SCALE));
    }

    /**
     * Creates a Money instance with the given amount as a string.
     *
     * @param amount the monetary amount as a string
     * @return a new Money instance
     * @throws IllegalArgumentException if the amount is negative or invalid
     * @throws NullPointerException if the amount is null
     */
    public static Money of(String amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        try {
            return of(new BigDecimal(amount));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + amount, e);
        }
    }

    /**
     * Returns the monetary amount.
     *
     * @return the amount as a BigDecimal with scale 2
     */
    public BigDecimal amount() {
        return amount;
    }

    /**
     * Adds another Money instance to this instance.
     *
     * @param other the Money to add
     * @return a new Money instance with the sum
     * @throws NullPointerException if other is null
     */
    public Money add(Money other) {
        Objects.requireNonNull(other, "Other money cannot be null");
        return new Money(amount.add(other.amount));
    }

    /**
     * Subtracts another Money instance from this instance.
     *
     * @param other the Money to subtract
     * @return a new Money instance with the difference
     * @throws IllegalArgumentException if the result would be negative
     * @throws NullPointerException if other is null
     */
    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Other money cannot be null");
        BigDecimal result = amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new IllegalArgumentException(NEGATIVE_AMOUNT_ERROR);
        }
        return new Money(result);
    }

    /**
     * Checks if this Money is zero.
     *
     * @return true if the amount is zero, false otherwise
     */
    public boolean isZero() {
        return amount.compareTo(ZERO) == 0;
    }

    /**
     * Checks if this Money is greater than zero.
     *
     * @return true if the amount is greater than zero, false otherwise
     */
    public boolean isPositive() {
        return amount.signum() > 0;
    }

    /**
     * Compares this Money with another Money instance.
     *
     * @param other the Money to compare with
     * @return a negative integer if this is less than other,
     *         zero if equal, or a positive integer if this is greater than other
     * @throws NullPointerException if other is null
     */
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "Other money cannot be null");
        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.equals(money.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
