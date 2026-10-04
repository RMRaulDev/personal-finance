package com.rauldev.personalfinance.application.readmodel;

import java.util.Objects;
import java.util.UUID;

public record ObligationSummary(
    UUID id,
    String name
) {
    public ObligationSummary {
        Objects.requireNonNull(id, "Obligation summary id cannot be null");
        Objects.requireNonNull(name, "Obligation summary name cannot be null");
    }
}
