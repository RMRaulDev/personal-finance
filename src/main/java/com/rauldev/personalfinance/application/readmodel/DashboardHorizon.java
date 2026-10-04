package com.rauldev.personalfinance.application.readmodel;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Days covered by the dashboard commitments, both inclusive.
 */
public record DashboardHorizon(
    LocalDate from,
    LocalDate to
) {
    public DashboardHorizon {
        Objects.requireNonNull(from, "Horizon start cannot be null");
        Objects.requireNonNull(to, "Horizon end cannot be null");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Horizon start cannot be after its end");
        }
    }
}
