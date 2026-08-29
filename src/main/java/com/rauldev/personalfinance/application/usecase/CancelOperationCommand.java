package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

public record CancelOperationCommand(
    UUID userId,
    UUID operationId
) {
    public CancelOperationCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(operationId, "Operation id cannot be null");
    }
}
