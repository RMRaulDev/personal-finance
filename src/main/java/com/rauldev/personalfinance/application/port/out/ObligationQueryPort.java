package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.ObligationDetails;

public interface ObligationQueryPort {
    /**
     * Returns all the user's obligations, active and archived, ordered by name and then by id.
     * Returns an empty list when the user has no obligations.
     */
    List<ObligationDetails> findByUserId(UUID userId);

    Optional<ObligationDetails> findByIdAndUserId(UUID obligationId, UUID userId);
}
