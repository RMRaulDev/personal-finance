package com.rauldev.personalfinance.entry.web.account;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.AccountDetails;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;

/**
 * Body of {@code GET /api/v1/accounts/{accountId}} and element of {@code GET /api/v1/accounts}.
 * The owner's user id is not exposed, and the status is the enum constant name ({@code ACTIVE},
 * {@code INACTIVE}).
 */
public record AccountResponse(UUID id, String name, long balanceCents, String status) {

    public static AccountResponse from(AccountDetails account) {
        Objects.requireNonNull(account, "Account details cannot be null");
        return new AccountResponse(
            account.id(), account.name(), MoneyCents.toCents(account.balance()), account.status().name());
    }
}
