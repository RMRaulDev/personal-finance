package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Money;

public record RegisterTransferCommand(
    UUID userId,
    UUID sourceAccountId,
    UUID targetAccountId,
    Money amount,
    LocalDate operationDate
) {
    public RegisterTransferCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(sourceAccountId, "Source account id cannot be null");
        Objects.requireNonNull(targetAccountId, "Target account id cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(operationDate, "Operation date cannot be null");
    }
}
