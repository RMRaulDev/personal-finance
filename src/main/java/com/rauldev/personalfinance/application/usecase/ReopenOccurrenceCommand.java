package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record ReopenOccurrenceCommand(
    UUID userId,
    UUID obligationId,
    LocalDate dueDate
) {
    public ReopenOccurrenceCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");
        Objects.requireNonNull(dueDate, "Due date cannot be null");
    }
}
