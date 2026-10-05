package com.rauldev.personalfinance.entry.web.common;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.AccountSummary;

public record AccountSummaryResponse(UUID id, String name) {

    public static AccountSummaryResponse from(AccountSummary account) {
        Objects.requireNonNull(account, "Account summary cannot be null");
        return new AccountSummaryResponse(account.id(), account.name());
    }
}
