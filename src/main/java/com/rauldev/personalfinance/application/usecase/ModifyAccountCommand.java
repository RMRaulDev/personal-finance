package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

public record ModifyAccountCommand(
    UUID userId,
    UUID accountId,
    String name
) {
    public ModifyAccountCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(accountId, "Account id cannot be null");
        Objects.requireNonNull(name, "Account name cannot be null");
    }
}
