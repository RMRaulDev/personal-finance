package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.ObligationQueryPort;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.application.readmodel.ObligationOccurrence;
import com.rauldev.personalfinance.application.readmodel.ObligationOccurrences;
import com.rauldev.personalfinance.application.readmodel.OccurrenceStatus;
import com.rauldev.personalfinance.domain.ObligationTimeline;

/**
 * Returns the occurrences of one of the user's obligations today: every overdue occurrence, the next unresolved
 * ones, and the most recent resolutions. The calendar math is the domain's ({@link ObligationTimeline}); an
 * archived obligation has no overdue or upcoming occurrences but keeps its resolutions.
 *
 * <p>Like {@link GetDashboard}, both reads run in one transaction (a consistent snapshot on a single connection).
 */
public final class GetObligationOccurrences {
    private static final int UPCOMING_LIMIT = 3;
    private static final int RESOLVED_LIMIT = 4;

    private final ObligationQueryPort obligationQueryPort;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public GetObligationOccurrences(
        ObligationQueryPort obligationQueryPort,
        TransactionManager transactionManager,
        Clock clock
    ) {
        this.obligationQueryPort = Objects.requireNonNull(obligationQueryPort, "Obligation query port cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    public ObligationOccurrences execute(GetObligationOccurrencesQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");

        return transactionManager.execute(() -> {
            LocalDate today = LocalDate.now(clock);

            ObligationDetails obligation = obligationQueryPort.findByIdAndUserId(query.obligationId(), query.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Obligation not found for user: " + query.obligationId()));

            List<ObligationOccurrence> resolvedOccurrences = obligationQueryPort.findResolvedOccurrences(
                obligation.id(), query.userId());
            Set<LocalDate> resolvedDates = resolvedOccurrences.stream()
                .map(ObligationOccurrence::dueDate)
                .collect(Collectors.toSet());
            ObligationTimeline timeline = new ObligationTimeline(obligation.recurrence(), obligation.status(),
                resolvedDates);

            List<ObligationOccurrence> overdue = unresolved(timeline.overdueDates(today), OccurrenceStatus.OVERDUE);
            List<ObligationOccurrence> upcoming = unresolved(timeline.upcomingDates(today, UPCOMING_LIMIT),
                OccurrenceStatus.PENDING);
            List<ObligationOccurrence> resolved = resolvedOccurrences.stream()
                .sorted(Comparator.comparing(ObligationOccurrence::dueDate).reversed())
                .limit(RESOLVED_LIMIT)
                .toList();

            return new ObligationOccurrences(overdue, upcoming, resolved);
        });
    }

    private static List<ObligationOccurrence> unresolved(List<LocalDate> dueDates, OccurrenceStatus status) {
        return dueDates.stream()
            .map(dueDate -> new ObligationOccurrence(dueDate, status, null))
            .toList();
    }
}
