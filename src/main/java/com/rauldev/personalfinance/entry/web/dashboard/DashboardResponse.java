package com.rauldev.personalfinance.entry.web.dashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.rauldev.personalfinance.application.readmodel.AvailableToSpend;
import com.rauldev.personalfinance.application.readmodel.Dashboard;
import com.rauldev.personalfinance.application.readmodel.DashboardHorizon;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;

/**
 * Body of {@code GET /api/v1/dashboard}.
 *
 * <p>{@code attention} keeps the priority order computed by the Core. {@code upcomingCommitments}
 * and {@code recent} are already limited by the Core; a user without data gets zero amounts and
 * empty lists, never {@code null}.
 */
public record DashboardResponse(
    HorizonResponse horizon,
    AvailableToSpendResponse availableToSpend,
    List<AttentionItemResponse> attention,
    List<UpcomingCommitmentResponse> upcomingCommitments,
    List<RecentActivityItemResponse> recent
) {

    public DashboardResponse {
        attention = List.copyOf(Objects.requireNonNull(attention, "Attention cannot be null"));
        upcomingCommitments = List.copyOf(Objects.requireNonNull(upcomingCommitments,
            "Upcoming commitments cannot be null"));
        recent = List.copyOf(Objects.requireNonNull(recent, "Recent activity cannot be null"));
    }

    public static DashboardResponse from(Dashboard dashboard) {
        Objects.requireNonNull(dashboard, "Dashboard cannot be null");
        return new DashboardResponse(
            HorizonResponse.from(dashboard.horizon()),
            AvailableToSpendResponse.from(dashboard.availableToSpend()),
            dashboard.attention().stream().map(AttentionItemResponse::from).toList(),
            dashboard.upcomingCommitments().stream().map(UpcomingCommitmentResponse::from).toList(),
            dashboard.recent().stream().map(RecentActivityItemResponse::from).toList());
    }

    /**
     * Days covered by the upcoming commitments, both inclusive.
     */
    public record HorizonResponse(LocalDate from, LocalDate to) {

        public static HorizonResponse from(DashboardHorizon horizon) {
            Objects.requireNonNull(horizon, "Horizon cannot be null");
            return new HorizonResponse(horizon.from(), horizon.to());
        }
    }

    /**
     * Balance of the active accounts against the committed amount. At most one of
     * {@code availableCents} and {@code shortfallCents} is positive.
     */
    public record AvailableToSpendResponse(
        long balanceCents,
        long committedCents,
        long availableCents,
        long shortfallCents
    ) {

        public static AvailableToSpendResponse from(AvailableToSpend availableToSpend) {
            Objects.requireNonNull(availableToSpend, "Available to spend cannot be null");
            return new AvailableToSpendResponse(
                MoneyCents.toCents(availableToSpend.balance()),
                MoneyCents.toCents(availableToSpend.committed()),
                MoneyCents.toCents(availableToSpend.available()),
                MoneyCents.toCents(availableToSpend.shortfall()));
        }
    }
}
