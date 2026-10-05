package com.rauldev.personalfinance.entry.web.common;

import java.util.Objects;

import com.rauldev.personalfinance.domain.Money;

/**
 * Converts between {@link Money} and the integer cents used in the HTTP API ({@code ...Cents}
 * fields). The currency (MXN) is implied.
 */
public final class MoneyCents {

    private MoneyCents() {
    }

    /**
     * @throws IllegalArgumentException if the cents are missing or negative
     */
    public static Money toMoney(Long cents, String field) {
        return Money.ofCents(RequiredFields.require(cents, field));
    }

    public static long toCents(Money money) {
        Objects.requireNonNull(money, "Money cannot be null");
        return money.amount().movePointRight(2).longValueExact();
    }
}
