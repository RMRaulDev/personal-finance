package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Money;

public record CompleteInitialSetupCommand(
    UUID userId,
    String accountName,
    Money openingBalance
) {
    public CompleteInitialSetupCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(accountName, "Account name cannot be null");
        Objects.requireNonNull(openingBalance, "Opening balance cannot be null");
    }
}
