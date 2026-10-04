package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Recurrence;

/**
 * Partial modification of an obligation. Every change field is optional: {@code null} keeps the current value.
 * {@code recurrence} replaces the whole calendar (frequency, start date and end date), so removing the end date is
 * expressed with a recurrence without one. At least one change is required.
 * <p>
 * When only one of {@code accountId} and {@code categoryId} is supplied, the other is taken from the obligation's
 * current value and both are re-validated, including the ownership, type, and active checks.
 * A supplied value equal to the current one is not treated as unchanged: the archived and overdue checks still run.
 */
public record ModifyObligationCommand(
    UUID userId,
    UUID obligationId,
    String name,
    Money amount,
    UUID accountId,
    UUID categoryId,
    Recurrence recurrence
) {
    public ModifyObligationCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");
        if (name == null && amount == null && accountId == null && categoryId == null && recurrence == null) {
            throw new IllegalArgumentException("At least one change is required");
        }
    }
}
