package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Commitment of one ACTIVE obligation: its overdue occurrences plus its pending occurrences in the horizon.
 *
 * @param pendingDates unresolved due dates within the horizon, ascending
 * @param paymentSourceInactive whether the account or the category of the obligation is inactive
 */
public record ObligationCommitment(UUID obligationId, Money committed, long overdueCount, Money overdueAmount,
                                   List<LocalDate> pendingDates, boolean paymentSourceInactive) {

    public ObligationCommitment {
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");
        Objects.requireNonNull(committed, "Committed amount cannot be null");
        if (overdueCount < 0) {
            throw new IllegalArgumentException("Overdue count cannot be negative");
        }
        Objects.requireNonNull(overdueAmount, "Overdue amount cannot be null");
        pendingDates = List.copyOf(Objects.requireNonNull(pendingDates, "Pending dates cannot be null"));
    }
}
