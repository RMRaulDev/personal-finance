package com.rauldev.personalfinance.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Obligation;

public interface ObligationRepository {
    Obligation create(Obligation obligation);

    Optional<Obligation> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    boolean existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID obligationId);

    Obligation update(Obligation obligation);
}
