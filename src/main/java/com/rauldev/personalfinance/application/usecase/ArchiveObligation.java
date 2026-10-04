package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.OccurrenceResolution;

public final class ArchiveObligation {
    private final ObligationRepository obligationRepository;
    private final OccurrenceResolutionRepository occurrenceResolutionRepository;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public ArchiveObligation(
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

    public UUID execute(ArchiveObligationCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            LocalDate today = LocalDate.now(clock);

            Obligation obligation = obligationRepository.findByIdAndUserId(command.obligationId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Obligation not found for user: " + command.obligationId()));

            List<OccurrenceResolution> resolutions = occurrenceResolutionRepository.findByObligationId(
                obligation.id());

            obligation.archive(today, resolutions);
            obligationRepository.update(obligation);
            return obligation.id();
        });
    }
}
