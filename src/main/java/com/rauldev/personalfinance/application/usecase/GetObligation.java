package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.ObligationQueryPort;
import com.rauldev.personalfinance.application.readmodel.ObligationDetails;

public final class GetObligation {
    private final ObligationQueryPort obligationQueryPort;

    public GetObligation(ObligationQueryPort obligationQueryPort) {
        this.obligationQueryPort = Objects.requireNonNull(obligationQueryPort, "Obligation query port cannot be null");
    }

    public ObligationDetails execute(GetObligationQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");

        return obligationQueryPort.findByIdAndUserId(query.obligationId(), query.userId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Obligation not found for user: " + query.obligationId()));
    }
}
