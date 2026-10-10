package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.ObligationQueryPort;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.application.readmodel.ObligationOverview;
import com.rauldev.personalfinance.domain.ResolutionSnapshot;

/**
 * Returns one of the user's obligations with the state of its occurrences today. Like {@link GetDashboard}, both
 * reads run in one transaction (a consistent snapshot on a single connection).
 *
 * <p>The resolved dates come from the same query as {@link ListObligations} (all the user's resolutions, filtered by
 * the obligation), so the list and the detail compute their fields from the same data and fail the same way.
 */
public final class GetObligation {
    private final ObligationQueryPort obligationQueryPort;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public GetObligation(
        ObligationQueryPort obligationQueryPort,
        TransactionManager transactionManager,
        Clock clock
    ) {
        this.obligationQueryPort = Objects.requireNonNull(obligationQueryPort, "Obligation query port cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    public ObligationOverview execute(GetObligationQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");

        return transactionManager.execute(() -> {
            LocalDate today = LocalDate.now(clock);

            ObligationDetails obligation = obligationQueryPort.findByIdAndUserId(query.obligationId(), query.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Obligation not found for user: " + query.obligationId()));

            Set<LocalDate> resolvedDates = obligationQueryPort.findResolutionsByUserId(query.userId()).stream()
                .filter(resolution -> resolution.obligationId().equals(obligation.id()))
                .map(ResolutionSnapshot::dueDate)
                .collect(Collectors.toSet());

            return ObligationOverview.of(obligation, resolvedDates, today);
        });
    }
}
