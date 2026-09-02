package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

public record GetAccountQuery(
    UUID userId,
    UUID accountId
) {
    public GetAccountQuery {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(accountId, "Account id cannot be null");
    }
}
