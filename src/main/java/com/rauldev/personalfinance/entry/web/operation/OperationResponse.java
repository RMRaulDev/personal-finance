package com.rauldev.personalfinance.entry.web.operation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationDetails;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationHistoryItem;
import com.rauldev.personalfinance.application.readmodel.TransferDetails;
import com.rauldev.personalfinance.entry.web.common.AccountSummaryResponse;
import com.rauldev.personalfinance.entry.web.common.CategorySummaryResponse;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;

/**
 * Body of {@code GET /api/v1/operations/{operationId}} and element of the operations history.
 *
 * <p>The shape depends on {@code type} ({@code INCOME}, {@code EXPENSE}, {@code TRANSFER}):
 * <ul>
 *   <li>Incomes and expenses carry {@code account} and {@code category}, a {@code status}
 *   ({@code ACTIVE} or {@code CANCELLED}), and {@code cancelledAt} (an ISO-8601 UTC instant, only
 *   when cancelled); {@code transfer} is {@code null}.</li>
 *   <li>Transfers carry {@code transfer} (source and target accounts); {@code account},
 *   {@code category}, {@code status}, and {@code cancelledAt} are {@code null} because transfers
 *   cannot be cancelled.</li>
 * </ul>
 * Absent parts are serialized as {@code null}, never omitted. Enums are written as their constant
 * name.
 */
public record OperationResponse(
    UUID id,
    String type,
    long amountCents,
    LocalDate operationDate,
    String status,
    Instant cancelledAt,
    AccountSummaryResponse account,
    CategorySummaryResponse category,
    TransferResponse transfer
) {

    public static OperationResponse from(FinancialOperationHistoryItem operation) {
        Objects.requireNonNull(operation, "Operation history item cannot be null");
        return new OperationResponse(
            operation.operationId(),
            operation.operationType().name(),
            MoneyCents.toCents(operation.amount()),
            operation.operationDate(),
            operation.status() == null ? null : operation.status().name(),
            operation.cancelledAt(),
            account(operation.account()),
            category(operation.category()),
            transfer(operation.transfer()));
    }

    public static OperationResponse from(FinancialOperationDetails operation) {
        Objects.requireNonNull(operation, "Operation details cannot be null");
        return new OperationResponse(
            operation.operationId(),
            operation.operationType().name(),
            MoneyCents.toCents(operation.amount()),
            operation.operationDate(),
            operation.status() == null ? null : operation.status().name(),
            operation.cancelledAt(),
            account(operation.account()),
            category(operation.category()),
            transfer(operation.transfer()));
    }

    private static AccountSummaryResponse account(AccountSummary account) {
        return account == null ? null : AccountSummaryResponse.from(account);
    }

    private static CategorySummaryResponse category(CategorySummary category) {
        return category == null ? null : CategorySummaryResponse.from(category);
    }

    private static TransferResponse transfer(TransferDetails transfer) {
        return transfer == null ? null : TransferResponse.from(transfer);
    }
}
