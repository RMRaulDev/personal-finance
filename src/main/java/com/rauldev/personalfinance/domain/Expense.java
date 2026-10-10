package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Expense extends FinancialOperation {
    private final UUID accountId;
    private final UUID categoryId;
    private OperationStatus status;

    /**
     * @param categoryId the category of the expense, or {@code null} if it has none
     */
    public Expense(UUID userId, Money amount, LocalDate operationDate, UUID accountId, UUID categoryId) {
        this(UUID.randomUUID(), userId, amount, operationDate, accountId, categoryId);
    }

    /**
     * @param categoryId the category of the expense, or {@code null} if it has none
     */
    public Expense(UUID id, UUID userId, Money amount, LocalDate operationDate, UUID accountId, UUID categoryId) {
        super(id, userId, amount, operationDate);
        this.accountId = Objects.requireNonNull(accountId, "Account id cannot be null");
        this.categoryId = categoryId;
        this.status = OperationStatus.ACTIVE;
    }

    /**
     * Registers a new expense. {@code category} is optional: when {@code null}, the category checks (same user,
     * type EXPENSE, active) are skipped and only the account checks apply.
     */
    public static Expense register(Account account, Category category, Money amount, LocalDate operationDate) {
        if (category == null) {
            OperationReferences.validateAccount(account);
        } else {
            OperationReferences.validate(account, category, CategoryType.EXPENSE,
                "Category type is not valid for an expense");
        }
        if (account.balance().compareTo(amount) < 0) {
            throw new BusinessRuleViolationException(BusinessRuleCode.INSUFFICIENT_BALANCE,
                "Account balance is insufficient");
        }
        return new Expense(account.userId(), amount, operationDate, account.id(),
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
                "Expense operation is already cancelled");
        }
    }

    public void cancel() {
        ensureCancellable();
        status = OperationStatus.CANCELLED;
    }
}
