package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.OccurrenceResolution;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.domain.ResolutionStatus;

class SkipOccurrenceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final Instant NOW = TODAY.atStartOfDay().toInstant(ZoneOffset.UTC);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final UUID userId = UUID.randomUUID();
    private final RecordingObligationRepository obligationRepository = new RecordingObligationRepository();
    private final RecordingResolutionRepository resolutionRepository = new RecordingResolutionRepository();
    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();

    private static LocalDate d(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    private Obligation obligation(Recurrence recurrence, ObligationStatus status) {
        Obligation obligation = new Obligation(UUID.randomUUID(), userId, "Rent", Money.ofCents(100),
            UUID.randomUUID(), UUID.randomUUID(), recurrence, status);
        obligationRepository.stored.add(obligation);
        return obligation;
    }

    /** Weekly (Thursdays) from 2026-08-06: 08-06 and 08-13 are overdue, 08-20 is today, 08-27 is future. */
    private Obligation weekly(ObligationStatus status) {
        return obligation(new Recurrence(Frequency.WEEKLY, d(2026, 8, 6), null), status);
    }

    private OccurrenceResolution existing(Obligation obligation, LocalDate dueDate, ResolutionStatus status) {
        UUID expenseId = status == ResolutionStatus.PAID ? UUID.randomUUID() : null;
        OccurrenceResolution resolution = new OccurrenceResolution(UUID.randomUUID(), obligation.id(), dueDate,
            status, expenseId, Instant.parse("2026-08-01T00:00:00Z"));
        resolutionRepository.resolutions.add(resolution);
        return resolution;
    }

    private static void assertRule(BusinessRuleCode code, String message, Runnable action) {
        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class, action::run);
        assertEquals(code, e.code());
        assertEquals(message, e.getMessage());
    }

    private SkipOccurrence useCase() {
        return new SkipOccurrence(obligationRepository, resolutionRepository, transactionManager, CLOCK);
    }

    private SkipOccurrenceCommand command(Obligation obligation, LocalDate dueDate) {
        return new SkipOccurrenceCommand(userId, obligation.id(), dueDate);
    }

    @Test
    void constructor_shouldThrowWhenObligationRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new SkipOccurrence(null, resolutionRepository, transactionManager, CLOCK));
        assertEquals("Obligation repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenOccurrenceResolutionRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new SkipOccurrence(obligationRepository, null, transactionManager, CLOCK));
        assertEquals("Occurrence resolution repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenTransactionManagerIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new SkipOccurrence(obligationRepository, resolutionRepository, null, CLOCK));
        assertEquals("Transaction manager cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenClockIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new SkipOccurrence(obligationRepository, resolutionRepository, transactionManager, null));
        assertEquals("Clock cannot be null", e.getMessage());
    }

    @Test
    void execute_shouldThrowWhenCommandIsNull() {
        assertThrows(NullPointerException.class, () -> useCase().execute(null));
    }

    @Test
    void execute_shouldPersistSkippedResolutionAndReturnObligationId() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);

        UUID result = useCase().execute(command(obligation, d(2026, 8, 13)));

        assertTrue(transactionManager.executed);
        assertEquals(obligation.id(), result);
        assertEquals(1, resolutionRepository.created.size());
        OccurrenceResolution saved = resolutionRepository.created.get(0);
        assertNotEquals(saved.id(), result);
        assertEquals(obligation.id(), saved.obligationId());
        assertEquals(d(2026, 8, 13), saved.dueDate());
        assertEquals(ResolutionStatus.SKIPPED, saved.status());
        assertTrue(saved.expenseId().isEmpty());
        assertEquals(NOW, saved.resolvedAt());
    }

    @Test
    void execute_shouldAllowSkippingAFutureScheduledDate() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);

        useCase().execute(command(obligation, d(2026, 8, 27)));

        assertEquals(1, resolutionRepository.created.size());
        assertEquals(d(2026, 8, 27), resolutionRepository.created.get(0).dueDate());
    }

    @Test
    void execute_shouldAllowSkippingTodaysOccurrence() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);

        useCase().execute(command(obligation, TODAY));

        assertEquals(1, resolutionRepository.created.size());
        OccurrenceResolution saved = resolutionRepository.created.get(0);
        assertEquals(TODAY, saved.dueDate());
        assertEquals(ResolutionStatus.SKIPPED, saved.status());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenObligationDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(new SkipOccurrenceCommand(userId, missingId, d(2026, 8, 13))));

        assertEquals("Obligation not found for user: " + missingId, e.getMessage());
        assertEquals(0, resolutionRepository.findByDateCalls);
        assertEquals(0, resolutionRepository.created.size());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenObligationBelongsToAnotherUser() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);

        assertThrows(ResourceNotFoundException.class, () -> useCase()
            .execute(new SkipOccurrenceCommand(UUID.randomUUID(), obligation.id(), d(2026, 8, 13))));

        assertEquals(0, resolutionRepository.findByDateCalls);
        assertEquals(0, resolutionRepository.created.size());
    }

    @Test
    void execute_shouldThrowWhenObligationIsArchived() {
        Obligation obligation = weekly(ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> useCase().execute(command(obligation, d(2026, 8, 13))));

        assertEquals(0, resolutionRepository.findByDateCalls);
        assertEquals(0, resolutionRepository.created.size());
    }

    @Test
    void execute_shouldThrowWhenDateIsNotOnTheCalendar() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);

        assertRule(BusinessRuleCode.OCCURRENCE_NOT_SCHEDULED, "Occurrence is not scheduled for the obligation",
            () -> useCase().execute(command(obligation, d(2026, 8, 14))));

        assertEquals(0, resolutionRepository.findByDateCalls);
        assertEquals(0, resolutionRepository.created.size());
    }

    @Test
    void execute_shouldCheckArchivedBeforeTheCalendar() {
        Obligation obligation = weekly(ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> useCase().execute(command(obligation, d(2026, 8, 14))));

        assertEquals(0, resolutionRepository.created.size());
    }

    @Test
    void execute_shouldThrowWhenOccurrenceIsAlreadySkipped() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);
        existing(obligation, d(2026, 8, 13), ResolutionStatus.SKIPPED);

        assertRule(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED,
            ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE,
            () -> useCase().execute(command(obligation, d(2026, 8, 13))));

        assertEquals(0, resolutionRepository.created.size());
    }

    @Test
    void execute_shouldThrowWhenOccurrenceIsAlreadyPaid() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);
        existing(obligation, d(2026, 8, 13), ResolutionStatus.PAID);

        assertRule(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED,
            ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE,
            () -> useCase().execute(command(obligation, d(2026, 8, 13))));

        assertEquals(0, resolutionRepository.created.size());
    }

    @Test
    void execute_shouldIgnoreResolutionsOfOtherDates() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);
        existing(obligation, d(2026, 8, 6), ResolutionStatus.SKIPPED);

        useCase().execute(command(obligation, d(2026, 8, 13)));

        assertEquals(1, resolutionRepository.created.size());
    }

    private static final class RecordingTransactionManager implements TransactionManager {
        private boolean executed;

        @Override
        public <T> T execute(Supplier<T> transactionalWork) {
            executed = true;
            return transactionalWork.get();
        }
    }

    private static final class RecordingObligationRepository implements ObligationRepository {
        private final List<Obligation> stored = new ArrayList<>();

        @Override
        public Obligation create(Obligation obligation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Obligation> findByIdAndUserId(UUID id, UUID userId) {
            return stored.stream().filter(o -> o.id().equals(id) && o.userId().equals(userId)).findFirst();
        }

        @Override
        public boolean existsByUserIdAndName(UUID userId, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID obligationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Obligation update(Obligation obligation) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingResolutionRepository implements OccurrenceResolutionRepository {
        private final List<OccurrenceResolution> resolutions = new ArrayList<>();
        private final List<OccurrenceResolution> created = new ArrayList<>();
        private final List<UUID> deleted = new ArrayList<>();
        private int findCalls;
        private int findByDateCalls;

        @Override
        public OccurrenceResolution create(OccurrenceResolution resolution) {
            created.add(resolution);
            resolutions.add(resolution);
            return resolution;
        }

        @Override
        public List<OccurrenceResolution> findByObligationId(UUID obligationId) {
            findCalls++;
            return resolutions.stream().filter(r -> r.obligationId().equals(obligationId)).toList();
        }

        @Override
        public Optional<OccurrenceResolution> findByObligationIdAndDueDate(UUID obligationId, LocalDate dueDate) {
            findByDateCalls++;
            return resolutions.stream()
                .filter(r -> r.obligationId().equals(obligationId) && r.dueDate().equals(dueDate)).findFirst();
        }

        @Override
        public Optional<OccurrenceResolution> findByExpenseId(UUID expenseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(UUID resolutionId) {
            deleted.add(resolutionId);
            resolutions.removeIf(r -> r.id().equals(resolutionId));
        }
    }
}
