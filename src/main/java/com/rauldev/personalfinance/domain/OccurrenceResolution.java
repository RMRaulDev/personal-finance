package com.rauldev.personalfinance.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Records what happened to one occurrence of an obligation: it was paid with an expense or skipped.
 *
 * The factories validate a new resolution against the current calendar of the obligation. The constructor does not,
 * so historical resolutions stay valid after the calendar is re-anchored.
 */
public final class OccurrenceResolution {
    private final UUID id;
    private final UUID obligationId;
    private final LocalDate dueDate;
    private final ResolutionStatus status;
    private final UUID expenseId;
    private final Instant resolvedAt;

    /**
     * @param expenseId the expense that paid the occurrence; required if PAID and {@code null} if SKIPPED
     */
    public OccurrenceResolution(UUID id, UUID obligationId, LocalDate dueDate, ResolutionStatus status,
                                UUID expenseId, Instant resolvedAt) {
        this.id = Objects.requireNonNull(id, "Resolution id cannot be null");
        this.obligationId = Objects.requireNonNull(obligationId, "Obligation id cannot be null");
        this.dueDate = Objects.requireNonNull(dueDate, "Due date cannot be null");
        this.status = Objects.requireNonNull(status, "Resolution status cannot be null");
        if (status == ResolutionStatus.PAID && expenseId == null) {
            throw new IllegalArgumentException("Paid resolution requires an expense id");
        }
        if (status == ResolutionStatus.SKIPPED && expenseId != null) {
            throw new IllegalArgumentException("Skipped resolution cannot have an expense id");
        }
        this.expenseId = expenseId;
        this.resolvedAt = Objects.requireNonNull(resolvedAt, "Resolved at cannot be null");
    }

    public static OccurrenceResolution paid(Obligation obligation, LocalDate dueDate, UUID expenseId,
                                            Instant resolvedAt) {
        validateNewResolution(obligation, dueDate);
        Objects.requireNonNull(expenseId, "Expense id cannot be null");
        return new OccurrenceResolution(UUID.randomUUID(), obligation.id(), dueDate, ResolutionStatus.PAID,
            expenseId, resolvedAt);
    }

    public static OccurrenceResolution skipped(Obligation obligation, LocalDate dueDate, Instant resolvedAt) {
        validateNewResolution(obligation, dueDate);
        return new OccurrenceResolution(UUID.randomUUID(), obligation.id(), dueDate, ResolutionStatus.SKIPPED,
            null, resolvedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID obligationId() {
        return obligationId;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public ResolutionStatus status() {
        return status;
    }

    public Optional<UUID> expenseId() {
        return Optional.ofNullable(expenseId);
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    /**
     * Only a skipped occurrence can be reopened directly. A paid one is reopened by cancelling its expense.
     */
    public void ensureReopenable() {
        if (status == ResolutionStatus.PAID) {
            throw new BusinessRuleViolationException(BusinessRuleCode.OCCURRENCE_PAID_NOT_REOPENABLE,
                "Paid occurrence can only be reopened by cancelling its expense");
        }
    }

    /**
     * Checks that a new resolution can be created for the date: the obligation is not archived
     * ({@code OBLIGATION_ARCHIVED}) and the date is on its current calendar ({@code OCCURRENCE_NOT_SCHEDULED}).
     * The factories call it; callers that must check before they have the expense id (paying) call it first.
     */
    public static void validateNewResolution(Obligation obligation, LocalDate dueDate) {
        Objects.requireNonNull(obligation, "Obligation cannot be null");
        Objects.requireNonNull(dueDate, "Due date cannot be null");
        obligation.ensureActive();
        if (!obligation.recurrence().isScheduled(dueDate)) {
            throw new BusinessRuleViolationException(BusinessRuleCode.OCCURRENCE_NOT_SCHEDULED,
                "Occurrence is not scheduled for the obligation");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OccurrenceResolution resolution)) {
            return false;
        }
        return id.equals(resolution.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
