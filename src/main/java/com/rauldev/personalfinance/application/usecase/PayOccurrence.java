package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.OccurrenceResolution;

/**
 * Pays one occurrence by registering an expense and saving a {@code PAID} resolution, in a single transaction.
 * Check order: operation date not after today, obligation (404), not archived, date on the current calendar (both
 * through {@link OccurrenceResolution#validateNewResolution}), no existing resolution for the date, then the expense
 * registration (account and category by user (404), then the checks of {@code Expense.register}). Future dates of the
 * calendar can be paid. A concurrent resolution of the same date is rejected by the repository with
 * {@code OCCURRENCE_ALREADY_RESOLVED}, and the transaction rolls back the expense and the balance change.
 */
public final class PayOccurrence {
    private final ObligationRepository obligationRepository;
    private final OccurrenceResolutionRepository occurrenceResolutionRepository;
    private final ExpenseRegistration expenseRegistration;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public PayOccurrence(
        ObligationRepository obligationRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        ExpenseRegistration expenseRegistration,
        TransactionManager transactionManager,
        Clock clock
    ) {
        this.obligationRepository = Objects.requireNonNull(obligationRepository,
            "Obligation repository cannot be null");
        this.occurrenceResolutionRepository = Objects.requireNonNull(occurrenceResolutionRepository,
            "Occurrence resolution repository cannot be null");
        this.expenseRegistration = Objects.requireNonNull(expenseRegistration,
            "Expense registration cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    /**
     * @return the id of the registered expense; cancelling it with {@code CancelOperation} reopens the occurrence
     */
    public UUID execute(PayOccurrenceCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            Instant now = Instant.now(clock);
            LocalDate today = LocalDate.now(clock);

            if (command.operationDate() != null && command.operationDate().isAfter(today)) {
                throw new IllegalArgumentException("Operation date cannot be after today");
            }

            Obligation obligation = obligationRepository.findByIdAndUserId(command.obligationId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Obligation not found for user: " + command.obligationId()));

            OccurrenceResolution.validateNewResolution(obligation, command.dueDate());

            if (occurrenceResolutionRepository.findByObligationIdAndDueDate(obligation.id(), command.dueDate())
                .isPresent()) {
                throw new BusinessRuleViolationException(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED,
                    ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE);
            }

            Expense expense = expenseRegistration.register(new RegisterExpenseCommand(
                command.userId(),
                Objects.requireNonNullElse(command.accountId(), obligation.accountId()),
                Objects.requireNonNullElse(command.categoryId(), obligation.categoryId()),
                Objects.requireNonNullElse(command.amount(), obligation.amount()),
                Objects.requireNonNullElse(command.operationDate(), today)
            ));

            occurrenceResolutionRepository.create(
                OccurrenceResolution.paid(obligation, command.dueDate(), expense.id(), now));
            return expense.id();
        });
    }
}
