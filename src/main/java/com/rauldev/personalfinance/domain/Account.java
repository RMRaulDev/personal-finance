package com.rauldev.personalfinance.domain;

import java.util.Objects;
import java.util.UUID;

public final class Account {
    private final UUID id;
    private final UUID userId;
    private String name;
    private Money balance;
    private AccountStatus status;

    public Account(UUID userId, String name) {
        this(UUID.randomUUID(), userId, name);
    }

    public Account(UUID id, UUID userId, String name) {
        this.id = Objects.requireNonNull(id, "Account id cannot be null");
        this.userId = Objects.requireNonNull(userId, "User id cannot be null");
        this.name = validateName(name);
        this.balance = Money.ofCents(0);
        this.status = AccountStatus.ACTIVE;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String name() {
        return name;
    }

    public Money balance() {
        return balance;
    }

    public AccountStatus status() {
        return status;
    }

    public void activate() {
        status = AccountStatus.ACTIVE;
    }

    public void deactivate() {
        status = AccountStatus.INACTIVE;
    }

    public void rename(String name) {
        this.name = validateName(name);
    }

    public void credit(Money amount) {
        balance = balance.add(requirePositive(amount));
    }

    public void debit(Money amount) {
        Money debitAmount = requirePositive(amount);
        if (balance.compareTo(debitAmount) < 0) {
            throw new IllegalStateException("Account balance is insufficient");
        }
        balance = balance.subtract(debitAmount);
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name, "Account name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }
        return name;
    }

    private static Money requirePositive(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return amount;
    }
}
