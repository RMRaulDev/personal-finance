package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Income extends FinancialOperation {
    private final UUID accountId;
    private final UUID categoryId;
    private OperationStatus status;

    public Income(UUID userId, Money amount, LocalDate operationDate, UUID accountId, UUID categoryId) {
        this(UUID.randomUUID(), userId, amount, operationDate, accountId, categoryId);
    }

    public Income(UUID id, UUID userId, Money amount, LocalDate operationDate, UUID accountId, UUID categoryId) {
        super(id, userId, amount, operationDate);
        this.accountId = Objects.requireNonNull(accountId, "Account id cannot be null");
        this.categoryId = Objects.requireNonNull(categoryId, "Category id cannot be null");
        this.status = OperationStatus.ACTIVE;
    }

    public static Income register(Account account, Category category, Money amount, LocalDate operationDate) {
        validateReferences(account, category, CategoryType.INCOME);
        return new Income(account.userId(), amount, operationDate, account.id(), category.id());
    }

    public UUID accountId() {
        return accountId;
    }

    public UUID categoryId() {
        return categoryId;
    }

    public OperationStatus status() {
        return status;
    }

    public void cancel() {
        status = OperationStatus.CANCELLED;
    }

    private static void validateReferences(Account account, Category category, CategoryType expectedType) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        if (!account.userId().equals(category.userId())) {
            throw new IllegalArgumentException("Account and category must belong to the same user");
        }
        if (category.type() != expectedType) {
            throw new IllegalArgumentException("Category type is not valid for this operation");
        }
    }
}
