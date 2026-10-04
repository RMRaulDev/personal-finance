package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

public record ArchiveObligationCommand(
    UUID userId,
    UUID obligationId
) {
    public ArchiveObligationCommand {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");
    }
}
