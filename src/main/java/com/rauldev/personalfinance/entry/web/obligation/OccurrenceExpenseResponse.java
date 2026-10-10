package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.OccurrenceExpense;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;

/**
 * The expense that paid an occurrence: {@code {id, amountCents, operationDate}}. {@code id} is the
 * operation id (cancel it with {@code POST /api/v1/operations/{operationId}/cancel} to undo the payment).
 */
public record OccurrenceExpenseResponse(UUID id, long amountCents, LocalDate operationDate) {

    public static OccurrenceExpenseResponse from(OccurrenceExpense expense) {
        Objects.requireNonNull(expense, "Occurrence expense cannot be null");
        return new OccurrenceExpenseResponse(
            expense.id(), MoneyCents.toCents(expense.amount()), expense.operationDate());
    }
}
