package com.rauldev.personalfinance.entry.web.operation;

import java.time.LocalDate;
import java.util.UUID;

import com.rauldev.personalfinance.application.usecase.RegisterIncomeCommand;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;
import com.rauldev.personalfinance.entry.web.common.RequiredFields;

/**
 * Body of {@code POST /api/v1/operations/incomes}. Fields are wrapper types so that a missing
 * JSON field arrives as {@code null} and answers 400 instead of defaulting to {@code 0}.
 * {@code operationDate} is an ISO-8601 date ({@code "2026-10-04"}). {@code categoryId} is optional: absent or
 * {@code null} registers the operation without a category.
 */
public record RegisterIncomeRequest(
    UUID accountId,
    UUID categoryId,
    Long amountCents,
    LocalDate operationDate
) {

    /**
     * @throws IllegalArgumentException if a required field is missing or {@code amountCents} is negative
     */
    public RegisterIncomeCommand toCommand(UUID userId) {
        return new RegisterIncomeCommand(
            userId,
            RequiredFields.require(accountId, "accountId"),
            categoryId,
            MoneyCents.toMoney(amountCents, "amountCents"),
            RequiredFields.require(operationDate, "operationDate"));
    }
}
