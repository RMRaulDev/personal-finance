package com.rauldev.personalfinance.application.readmodel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;

public record FinancialOperationDetails(
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
}
