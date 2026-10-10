package com.rauldev.personalfinance.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Obligation;

public interface ObligationRepository {
    Obligation create(Obligation obligation);

    Optional<Obligation> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Tells whether the user has an obligation (active or archived) with the same name ignoring case, as defined
     * by {@link Obligation#isSameName}.
     */
    boolean existsByUserIdAndName(UUID userId, String name);

    /**
     * Same as {@link #existsByUserIdAndName}, excluding the given obligation.
     */
    boolean existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID obligationId);

    Obligation update(Obligation obligation);
}
