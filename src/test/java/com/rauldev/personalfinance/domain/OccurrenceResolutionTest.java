package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OccurrenceResolutionTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final Instant RESOLVED_AT = Instant.parse("2026-08-20T12:00:00Z");

    private final UUID userId = UUID.randomUUID();
    private final Account account = new Account(userId, "Checking");
    private final Category category = new Category(userId, "Rent", CategoryType.EXPENSE);

    private static LocalDate d(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    private Obligation obligation(Recurrence recurrence, ObligationStatus status) {
        return new Obligation(UUID.randomUUID(), userId, "Rent", Money.ofCents(10000), account.id(), category.id(),
            recurrence, status);
    }

    private Obligation weeklyObligation() {
        return obligation(new Recurrence(Frequency.WEEKLY, d(2026, 8, 6), d(2026, 9, 3)), ObligationStatus.ACTIVE);
    }

    private static void assertRule(BusinessRuleCode code, String message, Runnable action) {
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, action::run);
        assertEquals(code, exception.code());
        assertEquals(message, exception.getMessage());
    }

    // Constructor

    @Test
    void constructorRejectsPaidResolutionWithoutExpense() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new OccurrenceResolution(UUID.randomUUID(), UUID.randomUUID(), TODAY, ResolutionStatus.PAID, null,
                RESOLVED_AT));

        assertEquals("Paid resolution requires an expense id", exception.getMessage());
    }

    @Test
    void constructorRejectsSkippedResolutionWithExpense() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new OccurrenceResolution(UUID.randomUUID(), UUID.randomUUID(), TODAY, ResolutionStatus.SKIPPED,
                UUID.randomUUID(), RESOLVED_AT));

        assertEquals("Skipped resolution cannot have an expense id", exception.getMessage());
    }

    @Test
    void constructorRejectsNullFields() {
        UUID id = UUID.randomUUID();
        UUID obligationId = UUID.randomUUID();

        assertThrows(NullPointerException.class,
            () -> new OccurrenceResolution(null, obligationId, TODAY, ResolutionStatus.SKIPPED, null, RESOLVED_AT));
        assertThrows(NullPointerException.class,
            () -> new OccurrenceResolution(id, null, TODAY, ResolutionStatus.SKIPPED, null, RESOLVED_AT));
        assertThrows(NullPointerException.class,
            () -> new OccurrenceResolution(id, obligationId, null, ResolutionStatus.SKIPPED, null, RESOLVED_AT));
        assertThrows(NullPointerException.class,
            () -> new OccurrenceResolution(id, obligationId, TODAY, null, null, RESOLVED_AT));
        assertThrows(NullPointerException.class,
            () -> new OccurrenceResolution(id, obligationId, TODAY, ResolutionStatus.SKIPPED, null, null));
    }

    @Test
    void constructorAllowsDateOffTheCalendar() {
        UUID obligationId = UUID.randomUUID();

        OccurrenceResolution resolution = new OccurrenceResolution(UUID.randomUUID(), obligationId, d(1999, 1, 1),
            ResolutionStatus.SKIPPED, null, RESOLVED_AT);

        assertEquals(d(1999, 1, 1), resolution.dueDate());
        assertEquals(obligationId, resolution.obligationId());
        assertEquals(RESOLVED_AT, resolution.resolvedAt());
    }

    @Test
    void constructorExposesExpenseIdAsOptional() {
        UUID expenseId = UUID.randomUUID();

        OccurrenceResolution paid = new OccurrenceResolution(UUID.randomUUID(), UUID.randomUUID(), TODAY,
            ResolutionStatus.PAID, expenseId, RESOLVED_AT);
        OccurrenceResolution skipped = new OccurrenceResolution(UUID.randomUUID(), UUID.randomUUID(), TODAY,
            ResolutionStatus.SKIPPED, null, RESOLVED_AT);

        assertEquals(Optional.of(expenseId), paid.expenseId());
        assertEquals(Optional.empty(), skipped.expenseId());
    }

    // paid

    @Test
    void paidCreatesResolutionForAScheduledDate() {
        Obligation obligation = weeklyObligation();
        UUID expenseId = UUID.randomUUID();

        OccurrenceResolution resolution = OccurrenceResolution.paid(obligation, d(2026, 8, 13), expenseId,
            RESOLVED_AT);

        assertEquals(obligation.id(), resolution.obligationId());
        assertEquals(d(2026, 8, 13), resolution.dueDate());
        assertEquals(ResolutionStatus.PAID, resolution.status());
        assertEquals(Optional.of(expenseId), resolution.expenseId());
        assertEquals(RESOLVED_AT, resolution.resolvedAt());
    }

    @Test
    void paidAllowsAFutureScheduledDate() {
        Obligation obligation = weeklyObligation();

        OccurrenceResolution resolution = OccurrenceResolution.paid(obligation, d(2026, 9, 3), UUID.randomUUID(),
            RESOLVED_AT);

        assertEquals(d(2026, 9, 3), resolution.dueDate());
    }

    @Test
    void paidRejectsArchivedObligationBeforeCheckingTheCalendar() {
        Obligation archived = obligation(new Recurrence(Frequency.WEEKLY, d(2026, 8, 6), null),
            ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> OccurrenceResolution.paid(archived, d(2026, 8, 7), UUID.randomUUID(), RESOLVED_AT));
    }

    @Test
    void paidRejectsDatesOffTheCalendar() {
        Obligation obligation = weeklyObligation();
        UUID expenseId = UUID.randomUUID();

        assertRule(BusinessRuleCode.OCCURRENCE_NOT_SCHEDULED, "Occurrence is not scheduled for the obligation",
            () -> OccurrenceResolution.paid(obligation, d(2026, 8, 14), expenseId, RESOLVED_AT));
        assertRule(BusinessRuleCode.OCCURRENCE_NOT_SCHEDULED, "Occurrence is not scheduled for the obligation",
            () -> OccurrenceResolution.paid(obligation, d(2026, 7, 30), expenseId, RESOLVED_AT));
        assertRule(BusinessRuleCode.OCCURRENCE_NOT_SCHEDULED, "Occurrence is not scheduled for the obligation",
            () -> OccurrenceResolution.paid(obligation, d(2026, 9, 10), expenseId, RESOLVED_AT));
    }

    @Test
    void paidRejectsNullExpenseId() {
        Obligation obligation = weeklyObligation();

        assertThrows(NullPointerException.class,
            () -> OccurrenceResolution.paid(obligation, d(2026, 8, 13), null, RESOLVED_AT));
    }

    // skipped

    @Test
    void skippedCreatesResolutionWithoutExpense() {
        Obligation obligation = weeklyObligation();

        OccurrenceResolution resolution = OccurrenceResolution.skipped(obligation, d(2026, 8, 20), RESOLVED_AT);

        assertEquals(obligation.id(), resolution.obligationId());
        assertEquals(d(2026, 8, 20), resolution.dueDate());
        assertEquals(ResolutionStatus.SKIPPED, resolution.status());
        assertEquals(Optional.empty(), resolution.expenseId());
    }

    @Test
    void skippedAllowsAFutureScheduledDate() {
        Obligation obligation = weeklyObligation();

        OccurrenceResolution resolution = OccurrenceResolution.skipped(obligation, d(2026, 9, 3), RESOLVED_AT);

        assertEquals(d(2026, 9, 3), resolution.dueDate());
    }

    @Test
    void skippedRejectsArchivedObligationBeforeCheckingTheCalendar() {
        Obligation archived = obligation(new Recurrence(Frequency.WEEKLY, d(2026, 8, 6), null),
            ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> OccurrenceResolution.skipped(archived, d(2026, 8, 7), RESOLVED_AT));
    }

    @Test
    void skippedRejectsDatesOffTheCalendar() {
        Obligation obligation = weeklyObligation();

        assertRule(BusinessRuleCode.OCCURRENCE_NOT_SCHEDULED, "Occurrence is not scheduled for the obligation",
            () -> OccurrenceResolution.skipped(obligation, d(2026, 8, 14), RESOLVED_AT));
        assertRule(BusinessRuleCode.OCCURRENCE_NOT_SCHEDULED, "Occurrence is not scheduled for the obligation",
            () -> OccurrenceResolution.skipped(obligation, d(2026, 7, 30), RESOLVED_AT));
        assertRule(BusinessRuleCode.OCCURRENCE_NOT_SCHEDULED, "Occurrence is not scheduled for the obligation",
            () -> OccurrenceResolution.skipped(obligation, d(2026, 9, 10), RESOLVED_AT));
    }

    // validateNewResolution

    @Test
    void validateNewResolutionAcceptsAScheduledDate() {
        Obligation obligation = weeklyObligation();

        assertDoesNotThrow(() -> OccurrenceResolution.validateNewResolution(obligation, d(2026, 8, 13)));
    }

    @Test
    void validateNewResolutionAcceptsAFutureScheduledDate() {
        Obligation obligation = weeklyObligation();

        assertDoesNotThrow(() -> OccurrenceResolution.validateNewResolution(obligation, d(2026, 9, 3)));
    }

    @Test
    void validateNewResolutionRejectsArchivedObligation() {
        Obligation archived = obligation(new Recurrence(Frequency.WEEKLY, d(2026, 8, 6), null),
            ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> OccurrenceResolution.validateNewResolution(archived, d(2026, 8, 13)));
    }

    @Test
    void validateNewResolutionRejectsDateOffTheCalendar() {
        Obligation obligation = weeklyObligation();

        assertRule(BusinessRuleCode.OCCURRENCE_NOT_SCHEDULED, "Occurrence is not scheduled for the obligation",
            () -> OccurrenceResolution.validateNewResolution(obligation, d(2026, 8, 14)));
    }

    @Test
    void validateNewResolutionReportsArchivedBeforeOffCalendar() {
        Obligation archived = obligation(new Recurrence(Frequency.WEEKLY, d(2026, 8, 6), null),
            ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> OccurrenceResolution.validateNewResolution(archived, d(2026, 8, 14)));
    }

    @Test
    void validateNewResolutionRejectsNullObligation() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> OccurrenceResolution.validateNewResolution(null, d(2026, 8, 13)));

        assertEquals("Obligation cannot be null", exception.getMessage());
    }

    @Test
    void validateNewResolutionRejectsNullDueDate() {
        Obligation obligation = weeklyObligation();

        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> OccurrenceResolution.validateNewResolution(obligation, null));

        assertEquals("Due date cannot be null", exception.getMessage());
    }

    // ensureReopenable

    @Test
    void ensureReopenableAcceptsSkippedResolution() {
        OccurrenceResolution skipped = new OccurrenceResolution(UUID.randomUUID(), UUID.randomUUID(), TODAY,
            ResolutionStatus.SKIPPED, null, RESOLVED_AT);

        assertDoesNotThrow(skipped::ensureReopenable);
    }

    @Test
    void ensureReopenableRejectsPaidResolution() {
        OccurrenceResolution paid = new OccurrenceResolution(UUID.randomUUID(), UUID.randomUUID(), TODAY,
            ResolutionStatus.PAID, UUID.randomUUID(), RESOLVED_AT);

        assertRule(BusinessRuleCode.OCCURRENCE_PAID_NOT_REOPENABLE,
            "Paid occurrence can only be reopened by cancelling its expense", paid::ensureReopenable);
    }

    // identity

    @Test
    void equalityIsByIdentifier() {
        UUID id = UUID.randomUUID();
        UUID obligationId = UUID.randomUUID();
        OccurrenceResolution first = new OccurrenceResolution(id, obligationId, TODAY, ResolutionStatus.SKIPPED,
            null, RESOLVED_AT);
        OccurrenceResolution same = new OccurrenceResolution(id, UUID.randomUUID(), TODAY.plusDays(1),
            ResolutionStatus.PAID, UUID.randomUUID(), RESOLVED_AT);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, new OccurrenceResolution(UUID.randomUUID(), obligationId, TODAY,
            ResolutionStatus.SKIPPED, null, RESOLVED_AT));
    }
}
