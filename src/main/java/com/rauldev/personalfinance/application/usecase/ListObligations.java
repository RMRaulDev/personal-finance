package com.rauldev.personalfinance.application.usecase;

import java.util.List;
import java.util.Objects;

import com.rauldev.personalfinance.application.port.out.ObligationQueryPort;
import com.rauldev.personalfinance.application.readmodel.ObligationDetails;

public final class ListObligations {
    private final ObligationQueryPort obligationQueryPort;

    public ListObligations(ObligationQueryPort obligationQueryPort) {
        this.obligationQueryPort = Objects.requireNonNull(obligationQueryPort, "Obligation query port cannot be null");
    }

    /**
     * Returns all the user's obligations, active and archived, ordered by name and then by id.
     */
    public List<ObligationDetails> execute(ListObligationsQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        return obligationQueryPort.findByUserId(query.userId());
    }
}
