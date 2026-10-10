package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Money;

public record CreateAccountCommand(
    UUID userId,
    String name,
    Money openingBalance
) {
    public CreateAccountCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(name, "Account name cannot be null");
        Objects.requireNonNull(openingBalance, "Opening balance cannot be null");
    }

    /**
     * Creates the command with an opening balance of zero.
     */
    public CreateAccountCommand(UUID userId, String name) {
        this(userId, name, Money.ofCents(0));
    }
}
