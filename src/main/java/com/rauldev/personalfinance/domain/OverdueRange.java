package com.rauldev.personalfinance.domain;

import java.time.LocalDate;

/**
 * The single definition of the overdue range of an obligation: from the start date of its calendar through
 * yesterday, both inclusive. Shared by {@link Obligation}, {@link ObligationTimeline}, and
 * {@link CommitmentCalculator}.
 */
record OverdueRange(LocalDate from, LocalDate to) {
    static OverdueRange of(Recurrence recurrence, LocalDate today) {
        return new OverdueRange(recurrence.startDate(), today.minusDays(1));
    }
}
