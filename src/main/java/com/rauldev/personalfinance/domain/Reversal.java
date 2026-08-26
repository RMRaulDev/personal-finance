package com.rauldev.personalfinance.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Reversal {
    private final UUID id;
    private final UUID userId;
    private final UUID originalOperationId;
    private final Money amount;
    private final Instant cancelledAt;

    public Reversal(FinancialOperation originalOperation, Instant cancelledAt) {
        this(UUID.randomUUID(), originalOperation, cancelledAt);
    }

    public Reversal(UUID id, FinancialOperation originalOperation, Instant cancelledAt) {
        this.id = Objects.requireNonNull(id, "Reversal id cannot be null");
        Objects.requireNonNull(originalOperation, "Original operation cannot be null");
        if (!(originalOperation instanceof Income) && !(originalOperation instanceof Expense)) {
            throw new IllegalArgumentException("Only income or expense operations can be reversed");
        }
        this.userId = originalOperation.userId();
        this.originalOperationId = originalOperation.id();
        this.amount = originalOperation.amount();
        this.cancelledAt = Objects.requireNonNull(cancelledAt, "Cancelled at cannot be null");
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public UUID originalOperationId() {
        return originalOperationId;
    }

    public Money amount() {
        return amount;
    }

    public Instant cancelledAt() {
        return cancelledAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Reversal reversal)) {
            return false;
        }
        return id.equals(reversal.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
