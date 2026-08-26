package com.rauldev.personalfinance.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Reversal;

public interface ReversalRepository {
    Optional<Reversal> findById(UUID id);

    Optional<Reversal> findByOriginalOperationId(UUID operationId);

    void save(Reversal reversal);
}
