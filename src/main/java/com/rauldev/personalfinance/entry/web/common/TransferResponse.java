package com.rauldev.personalfinance.entry.web.common;

import java.util.Objects;

import com.rauldev.personalfinance.application.readmodel.TransferDetails;

/**
 * The {@code transfer} part of an operation in the HTTP API: the two accounts of a transfer. Shared
 * by the operations endpoints ({@code OperationResponse}) and the dashboard's recent activity
 * items ({@code RecentActivityItemResponse}).
 */
public record TransferResponse(AccountSummaryResponse sourceAccount, AccountSummaryResponse targetAccount) {

    public static TransferResponse from(TransferDetails transfer) {
        Objects.requireNonNull(transfer, "Transfer details cannot be null");
        return new TransferResponse(
            AccountSummaryResponse.from(transfer.sourceAccount()),
            AccountSummaryResponse.from(transfer.targetAccount()));
    }
}
