package com.rauldev.personalfinance.application.readmodel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;

public record FinancialOperationHistoryItem(
    UUID operationId,
    OperationType operationType,
    Money amount,
    LocalDate operationDate,
    OperationStatus status,
    Instant cancelledAt,
    AccountSummary account,
    CategorySummary category,
    TransferDetails transfer
) {
    public FinancialOperationHistoryItem {
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
                Objects.requireNonNull(status, "Income and expense operations require a status");
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
                if (status != null) {
                    throw new IllegalArgumentException("Transfer operations must not include a status");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported operation type");
        }

        if (status != null) {
            switch (status) {
                case ACTIVE -> {
                    if (cancelledAt != null) {
                        throw new IllegalArgumentException("Active operations must not include cancelledAt");
                    }
                }
                case CANCELLED -> {
                    if (cancelledAt == null) {
                        throw new IllegalArgumentException("Cancelled operations require cancelledAt");
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported operation status");
            }
        } else if (cancelledAt != null) {
            throw new IllegalArgumentException("Transfer operations cannot include cancelledAt");
        }
    }
}
