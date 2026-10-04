package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Recurrence;

public record CreateObligationCommand(
    UUID userId,
    UUID accountId,
    UUID categoryId,
    String name,
    Money amount,
    Recurrence recurrence
) {
    public CreateObligationCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(accountId, "Account id cannot be null");
        Objects.requireNonNull(categoryId, "Category id cannot be null");
        Objects.requireNonNull(name, "Obligation name cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(recurrence, "Recurrence cannot be null");
    }
}
