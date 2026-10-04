package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.OccurrenceResolution;

/**
 * Skips one occurrence. Check order: obligation (404), not archived, date on the current calendar (both through
 * {@link OccurrenceResolution#skipped}), then no existing resolution for the date. A concurrent resolution of the
 * same date is rejected by the repository with the same code.
 */
public final class SkipOccurrence {
    private final ObligationRepository obligationRepository;
    private final OccurrenceResolutionRepository occurrenceResolutionRepository;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public SkipOccurrence(
        ObligationRepository obligationRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        TransactionManager transactionManager,
        Clock clock
    ) {
        this.obligationRepository = Objects.requireNonNull(obligationRepository,
            "Obligation repository cannot be null");
        this.occurrenceResolutionRepository = Objects.requireNonNull(occurrenceResolutionRepository,
            "Occurrence resolution repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    /**
     * @return the obligation id; clients address the skipped occurrence by obligation id and due date
     */
    public UUID execute(SkipOccurrenceCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            Instant now = Instant.now(clock);

            Obligation obligation = obligationRepository.findByIdAndUserId(command.obligationId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Obligation not found for user: " + command.obligationId()));

            OccurrenceResolution resolution = OccurrenceResolution.skipped(obligation, command.dueDate(), now);

            if (occurrenceResolutionRepository.findByObligationIdAndDueDate(obligation.id(), command.dueDate())
                .isPresent()) {
                throw new BusinessRuleViolationException(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED,
                    ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE);
            }

            occurrenceResolutionRepository.create(resolution);
            return obligation.id();
        });
    }
}
