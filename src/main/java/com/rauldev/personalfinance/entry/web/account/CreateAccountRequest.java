package com.rauldev.personalfinance.entry.web.account;

import java.util.UUID;

import com.rauldev.personalfinance.application.usecase.CreateAccountCommand;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;
import com.rauldev.personalfinance.entry.web.common.RequiredFields;

/**
 * Body of {@code POST /api/v1/accounts}. {@code openingBalanceCents} is optional: absent or
 * {@code null} means an opening balance of zero.
 */
public record CreateAccountRequest(String name, Long openingBalanceCents) {

    /**
     * @throws IllegalArgumentException if {@code name} is missing or {@code openingBalanceCents} is negative
     */
    public CreateAccountCommand toCommand(UUID userId) {
        Money openingBalance = openingBalanceCents == null
            ? Money.ofCents(0)
            : MoneyCents.toMoney(openingBalanceCents, "openingBalanceCents");
        return new CreateAccountCommand(userId, RequiredFields.require(name, "name"), openingBalance);
    }
}
