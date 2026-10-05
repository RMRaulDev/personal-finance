package com.rauldev.personalfinance.entry.web.operation;

import java.time.LocalDate;
import java.util.UUID;

import com.rauldev.personalfinance.application.usecase.RegisterTransferCommand;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;
import com.rauldev.personalfinance.entry.web.common.RequiredFields;

/**
 * Body of {@code POST /api/v1/operations/transfers}. Both accounts must belong to the current
 * user; a transfer has no category.
 */
public record RegisterTransferRequest(
    UUID sourceAccountId,
    UUID targetAccountId,
    Long amountCents,
    LocalDate operationDate
) {

    /**
     * @throws IllegalArgumentException if a field is missing or {@code amountCents} is negative
     */
    public RegisterTransferCommand toCommand(UUID userId) {
        return new RegisterTransferCommand(
            userId,
            RequiredFields.require(sourceAccountId, "sourceAccountId"),
            RequiredFields.require(targetAccountId, "targetAccountId"),
            MoneyCents.toMoney(amountCents, "amountCents"),
            RequiredFields.require(operationDate, "operationDate"));
    }
}
