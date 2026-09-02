package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.query.OperationSearchCriteria;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationDetails;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationHistoryItem;

public interface FinancialOperationQueryPort {
    List<FinancialOperationHistoryItem> search(OperationSearchCriteria criteria);

    Optional<FinancialOperationDetails> findDetailByIdAndUserId(UUID operationId, UUID userId);
}
