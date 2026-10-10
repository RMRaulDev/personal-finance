package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Income extends FinancialOperation {
    private final UUID accountId;
    private final UUID categoryId;
    private OperationStatus status;

    /**
     * @param categoryId the category of the income, or {@code null} if it has none
     */
    public Income(UUID userId, Money amount, LocalDate operationDate, UUID accountId, UUID categoryId) {
        this(UUID.randomUUID(), userId, amount, operationDate, accountId, categoryId);
    }

    /**
     * @param categoryId the category of the income, or {@code null} if it has none
     */
    public Income(UUID id, UUID userId, Money amount, LocalDate operationDate, UUID accountId, UUID categoryId) {
        super(id, userId, amount, operationDate);
        this.accountId = Objects.requireNonNull(accountId, "Account id cannot be null");
        this.categoryId = categoryId;
        this.status = OperationStatus.ACTIVE;
    }

    /**
     * Registers a new income. {@code category} is optional: when {@code null}, the category checks (same user,
     * type INCOME, active) are skipped and only the account checks apply.
     */
    public static Income register(Account account, Category category, Money amount, LocalDate operationDate) {
        if (category == null) {
            OperationReferences.validateAccount(account);
        } else {
            OperationReferences.validate(account, category, CategoryType.INCOME,
                "Category type is not valid for this operation");
        }
        return new Income(account.userId(), amount, operationDate, account.id(),
            category == null ? null : category.id());
    }

    public UUID accountId() {
        return accountId;
    }

    public Optional<UUID> categoryId() {
        return Optional.ofNullable(categoryId);
    }

    public OperationStatus status() {
        return status;
    }

    public void ensureCancellable() {
        if (status == OperationStatus.CANCELLED) {
            throw new BusinessRuleViolationException(BusinessRuleCode.OPERATION_ALREADY_CANCELLED,
                "Income operation is already cancelled");
        }
    }

    public void cancel() {
        ensureCancellable();
        status = OperationStatus.CANCELLED;
    }
}
