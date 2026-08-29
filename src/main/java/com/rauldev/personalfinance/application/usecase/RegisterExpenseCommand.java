package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Money;

public record RegisterExpenseCommand(
    UUID userId,
    UUID accountId,
    UUID categoryId,
    Money amount,
    LocalDate operationDate
) {
    public RegisterExpenseCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(accountId, "Account id cannot be null");
        Objects.requireNonNull(categoryId, "Category id cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(operationDate, "Operation date cannot be null");
    }
}
