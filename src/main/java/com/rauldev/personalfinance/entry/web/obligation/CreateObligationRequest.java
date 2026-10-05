package com.rauldev.personalfinance.entry.web.obligation;

import java.util.UUID;

import com.rauldev.personalfinance.application.usecase.CreateObligationCommand;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;
import com.rauldev.personalfinance.entry.web.common.RequiredFields;

/**
 * Body of {@code POST /api/v1/obligations}. Every field is required; {@code recurrence} is a nested
 * JSON object (see {@link RecurrenceRequest}). Fields are wrapper types so that a missing JSON
 * field arrives as {@code null} and answers 400.
 */
public record CreateObligationRequest(
    String name,
    Long amountCents,
    UUID accountId,
    UUID categoryId,
    RecurrenceRequest recurrence
) {

    /**
     * @throws IllegalArgumentException if a field is missing, {@code amountCents} is negative, or
     *     the recurrence is invalid
     */
    public CreateObligationCommand toCommand(UUID userId) {
        return new CreateObligationCommand(
            userId,
            RequiredFields.require(accountId, "accountId"),
            RequiredFields.require(categoryId, "categoryId"),
            RequiredFields.require(name, "name"),
            MoneyCents.toMoney(amountCents, "amountCents"),
            RequiredFields.require(recurrence, "recurrence").toRecurrence());
    }
}
