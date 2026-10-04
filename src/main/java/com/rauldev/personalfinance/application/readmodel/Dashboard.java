package com.rauldev.personalfinance.application.readmodel;

import java.util.List;
import java.util.Objects;

/**
 * Dashboard of a user. {@code attention} is complete and ordered by priority.
 */
public record Dashboard(
    DashboardHorizon horizon,
    AvailableToSpend availableToSpend,
    List<AttentionItem> attention,
    List<UpcomingCommitment> upcomingCommitments,
    List<RecentActivityItem> recent
) {
    public Dashboard {
        Objects.requireNonNull(horizon, "Horizon cannot be null");
        Objects.requireNonNull(availableToSpend, "Available to spend cannot be null");
        attention = List.copyOf(Objects.requireNonNull(attention, "Attention cannot be null"));
        upcomingCommitments = List.copyOf(Objects.requireNonNull(upcomingCommitments,
            "Upcoming commitments cannot be null"));
        recent = List.copyOf(Objects.requireNonNull(recent, "Recent activity cannot be null"));
    }
}
