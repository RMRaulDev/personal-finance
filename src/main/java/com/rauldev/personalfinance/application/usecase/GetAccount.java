package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountQueryPort;
import com.rauldev.personalfinance.application.readmodel.AccountDetails;

public final class GetAccount {
    private final AccountQueryPort accountQueryPort;

    public GetAccount(AccountQueryPort accountQueryPort) {
        this.accountQueryPort = Objects.requireNonNull(accountQueryPort, "Account query port cannot be null");
    }

    public AccountDetails execute(GetAccountQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");

        return accountQueryPort.findByIdAndUserId(query.accountId(), query.userId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Account not found for user: " + query.accountId()));
    }
}
