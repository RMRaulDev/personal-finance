package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.OccurrenceResolution;

/**
 * Skips every overdue occurrence (scheduled from the start date through yesterday, without a resolution) in a single
 * transaction. An archived obligation fails with {@code OBLIGATION_ARCHIVED} even when nothing is overdue; an active
 * obligation without overdue occurrences is a no-op. If any insert fails (e.g. a concurrent resolution), nothing is
 * persisted.
 */
public final class SkipOverdueOccurrences {
    private final ObligationRepository obligationRepository;
    private final OccurrenceResolutionRepository occurrenceResolutionRepository;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public SkipOverdueOccurrences(
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
     * @return the skipped due dates in ascending order; empty if nothing was overdue
     */
    public List<LocalDate> execute(SkipOverdueOccurrencesCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            Instant now = Instant.now(clock);
            LocalDate today = LocalDate.ofInstant(now, clock.getZone());

            Obligation obligation = obligationRepository.findByIdAndUserId(command.obligationId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Obligation not found for user: " + command.obligationId()));

            obligation.ensureActive();

            List<OccurrenceResolution> resolutions = occurrenceResolutionRepository.findByObligationId(
                obligation.id());

            List<LocalDate> overdueDates = obligation.overdueDates(today, resolutions);

            for (LocalDate dueDate : overdueDates) {
                occurrenceResolutionRepository.create(OccurrenceResolution.skipped(obligation, dueDate, now));
            }
            return overdueDates;
        });
    }
}
