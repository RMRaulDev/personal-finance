package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public abstract class FinancialOperation {
    private final UUID id;
    private final UUID userId;
    private final Money amount;
    private final LocalDate operationDate;

    protected FinancialOperation(UUID userId, Money amount, LocalDate operationDate) {
        this(UUID.randomUUID(), userId, amount, operationDate);
    }

    protected FinancialOperation(UUID id, UUID userId, Money amount, LocalDate operationDate) {
        this.id = Objects.requireNonNull(id, "Operation id cannot be null");
        this.userId = Objects.requireNonNull(userId, "User id cannot be null");
        this.amount = requirePositive(amount);
        this.operationDate = Objects.requireNonNull(operationDate, "Operation date cannot be null");
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public Money amount() {
        return amount;
    }

    public LocalDate operationDate() {
        return operationDate;
    }

    private static Money requirePositive(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Operation amount must be greater than zero");
        }
        return amount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        FinancialOperation operation = (FinancialOperation) other;
        return id.equals(operation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }
}
