package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;
import java.util.UUID;

import com.rauldev.personalfinance.application.usecase.PayOccurrenceCommand;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;

/**
 * Optional body of {@code POST /api/v1/obligations/{obligationId}/occurrences/{dueDate}/pay}. Every
 * field is optional and overrides one default of the payment: {@code amountCents} (the obligation's
 * amount; must be greater than zero), {@code operationDate} (today; cannot be in the future),
 * {@code accountId} and {@code categoryId} (the obligation's account and category, for example to
 * unblock a payment whose account is inactive).
 */
public record PayOccurrenceRequest(
    Long amountCents,
    LocalDate operationDate,
    UUID accountId,
    UUID categoryId
) {

    /**
     * @throws IllegalArgumentException if {@code amountCents} is zero or negative
     */
    public PayOccurrenceCommand toCommand(UUID userId, UUID obligationId, LocalDate dueDate) {
        return new PayOccurrenceCommand(
            userId,
            obligationId,
            dueDate,
            amountCents == null ? null : MoneyCents.toMoney(amountCents, "amountCents"),
            operationDate,
            accountId,
            categoryId);
    }
}
