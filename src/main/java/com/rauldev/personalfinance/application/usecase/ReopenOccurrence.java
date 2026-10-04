package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.OccurrenceResolution;

/**
 * Reopens a skipped occurrence by deleting its resolution. Check order: obligation (404), not archived
 * ({@code OBLIGATION_ARCHIVED}), resolution for the date (404), not paid ({@code OCCURRENCE_PAID_NOT_REOPENABLE}).
 * The date is not checked against the current calendar, so a skipped resolution left off the calendar by a
 * re-anchor can be removed.
 */
public final class ReopenOccurrence {
    private final ObligationRepository obligationRepository;
    private final OccurrenceResolutionRepository occurrenceResolutionRepository;
    private final TransactionManager transactionManager;

    public ReopenOccurrence(
        ObligationRepository obligationRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        TransactionManager transactionManager
    ) {
        this.obligationRepository = Objects.requireNonNull(obligationRepository,
            "Obligation repository cannot be null");
        this.occurrenceResolutionRepository = Objects.requireNonNull(occurrenceResolutionRepository,
            "Occurrence resolution repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }

    /**
     * @return the id of the obligation
     */
    public UUID execute(ReopenOccurrenceCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            Obligation obligation = obligationRepository.findByIdAndUserId(command.obligationId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Obligation not found for user: " + command.obligationId()));

            obligation.ensureActive();

            OccurrenceResolution resolution = occurrenceResolutionRepository
                .findByObligationIdAndDueDate(obligation.id(), command.dueDate())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Occurrence resolution not found for obligation " + obligation.id() + " on "
                        + command.dueDate()));

            resolution.ensureReopenable();

            occurrenceResolutionRepository.delete(resolution.id());
            return obligation.id();
        });
    }
}
