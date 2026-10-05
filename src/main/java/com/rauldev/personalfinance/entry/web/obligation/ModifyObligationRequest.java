package com.rauldev.personalfinance.entry.web.obligation;

import java.util.UUID;

import com.rauldev.personalfinance.application.usecase.ModifyObligationCommand;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;

/**
 * Body of {@code PATCH /api/v1/obligations/{obligationId}}: a partial update. A missing or
 * {@code null} field keeps the current value, so a field cannot be cleared through this request.
 *
 * <p>{@code recurrence}, when present, replaces the whole calendar: its {@code frequency} and
 * {@code startDate} are required, and a missing or {@code null} {@code endDate} inside it removes
 * the current end date. A body with no change at all (for example {@code {}}) is rejected by the
 * Core with 400 ("At least one change is required").
 */
public record ModifyObligationRequest(
    String name,
    Long amountCents,
    UUID accountId,
    UUID categoryId,
    RecurrenceRequest recurrence
) {

    /**
     * @throws IllegalArgumentException if no field is present, {@code amountCents} is negative, or
     *     the recurrence is incomplete or invalid
     */
    public ModifyObligationCommand toCommand(UUID userId, UUID obligationId) {
        return new ModifyObligationCommand(
            userId,
            obligationId,
            name,
            amountCents == null ? null : MoneyCents.toMoney(amountCents, "amountCents"),
            accountId,
            categoryId,
            recurrence == null ? null : recurrence.toRecurrence());
    }
}
