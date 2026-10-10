package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.rauldev.personalfinance.application.port.out.ObligationQueryPort;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.application.readmodel.ObligationOverview;
import com.rauldev.personalfinance.domain.ResolutionSnapshot;

/**
 * Lists the user's obligations with the state of their occurrences today.
 *
 * <p>Like {@link GetDashboard}, both reads run in one transaction so that the obligations and their resolutions
 * come from a single connection and a consistent snapshot of the database.
 */
public final class ListObligations {
    private final ObligationQueryPort obligationQueryPort;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public ListObligations(
        ObligationQueryPort obligationQueryPort,
        TransactionManager transactionManager,
        Clock clock
    ) {
        this.obligationQueryPort = Objects.requireNonNull(obligationQueryPort, "Obligation query port cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    /**
     * Returns all the user's obligations, active and archived, ordered by name and then by id.
     */
    public List<ObligationOverview> execute(ListObligationsQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");

        return transactionManager.execute(() -> {
            LocalDate today = LocalDate.now(clock);

            List<ObligationDetails> obligations = obligationQueryPort.findByUserId(query.userId());
            Map<UUID, Set<LocalDate>> resolvedDatesByObligation =
                obligationQueryPort.findResolutionsByUserId(query.userId()).stream()
                    .collect(Collectors.groupingBy(ResolutionSnapshot::obligationId,
                        Collectors.mapping(ResolutionSnapshot::dueDate, Collectors.toSet())));

            return obligations.stream()
                .map(obligation -> ObligationOverview.of(obligation,
                    resolvedDatesByObligation.getOrDefault(obligation.id(), Set.of()), today))
                .toList();
        });
    }
}
