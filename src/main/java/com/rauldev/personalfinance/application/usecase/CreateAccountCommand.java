package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

public record CreateAccountCommand(
    UUID userId,
    String name
) {
    public CreateAccountCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(name, "Account name cannot be null");
    }
}
