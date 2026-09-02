package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.FinancialOperationQueryPort;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationDetails;

public final class GetOperationDetails {
    private final FinancialOperationQueryPort financialOperationQueryPort;

    public GetOperationDetails(FinancialOperationQueryPort financialOperationQueryPort) {
        this.financialOperationQueryPort = Objects.requireNonNull(
            financialOperationQueryPort,
            "Financial operation query port cannot be null");
    }

    public FinancialOperationDetails execute(GetOperationDetailsQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");

        return financialOperationQueryPort.findDetailByIdAndUserId(query.operationId(), query.userId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Operation not found for user: " + query.operationId()));
    }
}
