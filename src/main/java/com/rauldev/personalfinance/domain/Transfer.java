package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Transfer extends FinancialOperation {
    private final UUID sourceAccountId;
    private final UUID targetAccountId;

    public Transfer(UUID userId, Money amount, LocalDate operationDate,
                    UUID sourceAccountId, UUID targetAccountId) {
        this(UUID.randomUUID(), userId, amount, operationDate, sourceAccountId, targetAccountId);
    }

    public Transfer(UUID id, UUID userId, Money amount, LocalDate operationDate,
                    UUID sourceAccountId, UUID targetAccountId) {
        super(id, userId, amount, operationDate);
        this.sourceAccountId = requireDifferentId(sourceAccountId, targetAccountId);
        this.targetAccountId = Objects.requireNonNull(targetAccountId, "Target account id cannot be null");
    }

    public static Transfer register(Account sourceAccount, Account targetAccount,
                                    Money amount, LocalDate operationDate) {
        Objects.requireNonNull(sourceAccount, "Source account cannot be null");
        Objects.requireNonNull(targetAccount, "Target account cannot be null");
        if (sourceAccount.id().equals(targetAccount.id())) {
            throw new IllegalArgumentException("Source and target accounts must be different");
        }
        if (!sourceAccount.userId().equals(targetAccount.userId())) {
            throw new IllegalArgumentException("Accounts must belong to the same user");
        }
        if (sourceAccount.status() != AccountStatus.ACTIVE || targetAccount.status() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Both accounts must be active");
        }
        if (sourceAccount.balance().compareTo(amount) < 0) {
            throw new IllegalStateException("Source account balance is insufficient");
        }
        return new Transfer(sourceAccount.userId(), amount, operationDate,
            sourceAccount.id(), targetAccount.id());
    }

    public UUID sourceAccountId() {
        return sourceAccountId;
    }

    public UUID targetAccountId() {
        return targetAccountId;
    }

    private static UUID requireDifferentId(UUID sourceAccountId, UUID targetAccountId) {
        Objects.requireNonNull(sourceAccountId, "Source account id cannot be null");
        Objects.requireNonNull(targetAccountId, "Target account id cannot be null");
        if (sourceAccountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("Source and target accounts must be different");
        }
        return sourceAccountId;
    }
}
