package com.rauldev.personalfinance.application.usecase;

import java.util.List;
import java.util.Objects;

import com.rauldev.personalfinance.application.port.out.FinancialOperationQueryPort;
import com.rauldev.personalfinance.application.query.OperationSearchCriteria;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationHistoryItem;

public final class GetOperationHistory {
    private final FinancialOperationQueryPort financialOperationQueryPort;

    public GetOperationHistory(FinancialOperationQueryPort financialOperationQueryPort) {
        this.financialOperationQueryPort = Objects.requireNonNull(
            financialOperationQueryPort,
            "Financial operation query port cannot be null");
    }

    public List<FinancialOperationHistoryItem> execute(OperationSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "Criteria cannot be null");
        return financialOperationQueryPort.search(criteria);
    }
}
