package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

public record GetOperationDetailsQuery(
    UUID userId,
    UUID operationId
) {
    public GetOperationDetailsQuery {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(operationId, "Operation id cannot be null");
    }
}
