package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.query.OperationSearchCriteria;
import com.rauldev.personalfinance.domain.FinancialOperation;

public interface FinancialOperationRepository {
    Optional<FinancialOperation> findById(UUID id);

    Optional<FinancialOperation> findByIdAndUserId(UUID id, UUID userId);

    List<FinancialOperation> search(OperationSearchCriteria criteria);

    void save(FinancialOperation operation);
}
