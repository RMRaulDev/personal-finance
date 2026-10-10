package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.application.readmodel.ObligationOccurrence;
import com.rauldev.personalfinance.domain.ResolutionSnapshot;

public interface ObligationQueryPort {
    /**
     * Returns all the user's obligations, active and archived, ordered by name and then by id.
     * Returns an empty list when the user has no obligations.
     */
    List<ObligationDetails> findByUserId(UUID userId);

    Optional<ObligationDetails> findByIdAndUserId(UUID obligationId, UUID userId);

    /**
     * Returns the resolutions (PAID and SKIPPED) of all the user's obligations, active and archived.
     */
    List<ResolutionSnapshot> findResolutionsByUserId(UUID userId);

    /**
     * Returns every resolution of the obligation as a {@code PAID} occurrence (with its expense) or a
     * {@code SKIPPED} one, ordered by due date descending. Returns an empty list when the obligation has none or
     * does not belong to the user.
     */
    List<ObligationOccurrence> findResolvedOccurrences(UUID obligationId, UUID userId);
}
