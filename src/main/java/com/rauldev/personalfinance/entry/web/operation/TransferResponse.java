package com.rauldev.personalfinance.entry.web.operation;

import java.util.Objects;

import com.rauldev.personalfinance.application.readmodel.TransferDetails;
import com.rauldev.personalfinance.entry.web.common.AccountSummaryResponse;

/**
 * The {@code transfer} part of an {@link OperationResponse}: the two accounts of a transfer.
 */
public record TransferResponse(AccountSummaryResponse sourceAccount, AccountSummaryResponse targetAccount) {

    public static TransferResponse from(TransferDetails transfer) {
        Objects.requireNonNull(transfer, "Transfer details cannot be null");
        return new TransferResponse(
            AccountSummaryResponse.from(transfer.sourceAccount()),
            AccountSummaryResponse.from(transfer.targetAccount()));
    }
}
