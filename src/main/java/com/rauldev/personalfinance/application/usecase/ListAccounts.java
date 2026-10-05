package com.rauldev.personalfinance.application.usecase;

import java.util.List;
import java.util.Objects;

import com.rauldev.personalfinance.application.port.out.AccountQueryPort;
import com.rauldev.personalfinance.application.readmodel.AccountDetails;

public final class ListAccounts {
    private final AccountQueryPort accountQueryPort;

    public ListAccounts(AccountQueryPort accountQueryPort) {
        this.accountQueryPort = Objects.requireNonNull(accountQueryPort, "Account query port cannot be null");
    }

    /**
     * Returns all the user's accounts, active and inactive, ordered by name and then by id.
     */
    public List<AccountDetails> execute(ListAccountsQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        return accountQueryPort.findByUserId(query.userId());
    }
}
