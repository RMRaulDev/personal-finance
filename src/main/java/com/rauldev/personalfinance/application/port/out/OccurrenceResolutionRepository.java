package com.rauldev.personalfinance.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.domain.OccurrenceResolution;

/**
 * Persists occurrence resolutions. No method checks the user: callers must first load the obligation with
 * {@link ObligationRepository#findByIdAndUserId}, or, for {@link #findByExpenseId}, the expense scoped by user.
 */
public interface OccurrenceResolutionRepository {
    OccurrenceResolution create(OccurrenceResolution resolution);

    /**
     * Returns every resolution of the obligation, ordered by due date.
     */
    List<OccurrenceResolution> findByObligationId(UUID obligationId);

    Optional<OccurrenceResolution> findByObligationIdAndDueDate(UUID obligationId, LocalDate dueDate);

    Optional<OccurrenceResolution> findByExpenseId(UUID expenseId);

    /**
     * Deletes the resolution. Deleting a missing id is a no-op.
     */
    void delete(UUID resolutionId);
}
