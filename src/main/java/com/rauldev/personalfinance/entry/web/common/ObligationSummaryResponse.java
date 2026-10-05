package com.rauldev.personalfinance.entry.web.common;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.ObligationSummary;

public record ObligationSummaryResponse(UUID id, String name) {

    public static ObligationSummaryResponse from(ObligationSummary obligation) {
        Objects.requireNonNull(obligation, "Obligation summary cannot be null");
        return new ObligationSummaryResponse(obligation.id(), obligation.name());
    }
}
