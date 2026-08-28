package com.rauldev.personalfinance.application.readmodel;

public record TransferDetails(
    AccountSummary sourceAccount,
    AccountSummary targetAccount
) {
}
