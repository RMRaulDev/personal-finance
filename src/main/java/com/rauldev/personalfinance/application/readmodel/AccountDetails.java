package com.rauldev.personalfinance.application.readmodel;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.Money;

public record AccountDetails(
    UUID id,
    UUID userId,
    String name,
    Money balance,
    AccountStatus status
) {
    public AccountDetails {
        Objects.requireNonNull(id, "Account id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(name, "Account name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }
        Objects.requireNonNull(balance, "Account balance cannot be null");
        Objects.requireNonNull(status, "Account status cannot be null");
    }
}
