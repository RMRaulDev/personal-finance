package com.rauldev.personalfinance.application.readmodel;

import java.util.Objects;

public record TransferDetails(
    AccountSummary sourceAccount,
    AccountSummary targetAccount
) {
    public TransferDetails {
        Objects.requireNonNull(sourceAccount, "Source account cannot be null");
        Objects.requireNonNull(targetAccount, "Target account cannot be null");
    }
}
