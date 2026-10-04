package com.rauldev.personalfinance.application.readmodel;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.domain.Money;

/**
 * A recent active (not cancelled) operation.
 *
 * @param obligation the obligation whose occurrence this expense paid, or {@code null}
 */
public record RecentActivityItem(
    UUID operationId,
    OperationType operationType,
    Money amount,
    LocalDate operationDate,
    AccountSummary account,
    CategorySummary category,
    TransferDetails transfer,
    ObligationSummary obligation
) {
    public RecentActivityItem {
        Objects.requireNonNull(operationId, "Operation id cannot be null");
        Objects.requireNonNull(operationType, "Operation type cannot be null");
        Objects.requireNonNull(amount, "Operation amount cannot be null");
        Objects.requireNonNull(operationDate, "Operation date cannot be null");

        switch (operationType) {
            case INCOME, EXPENSE -> {
                if (account == null) {
                    throw new IllegalArgumentException("Income and expense operations require an account");
                }
                if (category == null) {
                    throw new IllegalArgumentException("Income and expense operations require a category");
                }
                if (transfer != null) {
                    throw new IllegalArgumentException("Income and expense operations cannot include transfer details");
                }
            }
            case TRANSFER -> {
                if (account != null) {
                    throw new IllegalArgumentException("Transfer operations must not include account");
                }
                if (category != null) {
                    throw new IllegalArgumentException("Transfer operations must not include category");
                }
                if (transfer == null) {
                    throw new IllegalArgumentException("Transfer operations require transfer details");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported operation type");
        }

        if (obligation != null && operationType != OperationType.EXPENSE) {
            throw new IllegalArgumentException("Only expense operations can reference an obligation");
        }
    }
}
