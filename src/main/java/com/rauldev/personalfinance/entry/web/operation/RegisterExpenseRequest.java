package com.rauldev.personalfinance.entry.web.operation;

import java.time.LocalDate;
import java.util.UUID;

import com.rauldev.personalfinance.application.usecase.RegisterExpenseCommand;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;
import com.rauldev.personalfinance.entry.web.common.RequiredFields;

/**
 * Body of {@code POST /api/v1/operations/expenses}. Fields are wrapper types so that a missing
 * JSON field arrives as {@code null} and answers 400 instead of defaulting to {@code 0}.
 * {@code operationDate} is an ISO-8601 date ({@code "2026-10-04"}).
 */
public record RegisterExpenseRequest(
    UUID accountId,
    UUID categoryId,
    Long amountCents,
    LocalDate operationDate
) {

    /**
     * @throws IllegalArgumentException if a field is missing or {@code amountCents} is negative
     */
    public RegisterExpenseCommand toCommand(UUID userId) {
        return new RegisterExpenseCommand(
            userId,
            RequiredFields.require(accountId, "accountId"),
            RequiredFields.require(categoryId, "categoryId"),
            MoneyCents.toMoney(amountCents, "amountCents"),
            RequiredFields.require(operationDate, "operationDate"));
    }
}
