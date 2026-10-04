package com.rauldev.personalfinance.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

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

class ReopenOccurrenceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);

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

    private ReopenOccurrence useCase() {
        return new ReopenOccurrence(obligationRepository, resolutionRepository, transactionManager);
    }

    private ReopenOccurrenceCommand command(Obligation obligation, LocalDate dueDate) {
        return new ReopenOccurrenceCommand(userId, obligation.id(), dueDate);
    }

    @Test
    void constructor_shouldThrowWhenObligationRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ReopenOccurrence(null, resolutionRepository, transactionManager));
        assertEquals("Obligation repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenOccurrenceResolutionRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ReopenOccurrence(obligationRepository, null, transactionManager));
        assertEquals("Occurrence resolution repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenTransactionManagerIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ReopenOccurrence(obligationRepository, resolutionRepository, null));
        assertEquals("Transaction manager cannot be null", e.getMessage());
    }

    @Test
    void execute_shouldThrowWhenCommandIsNull() {
        assertThrows(NullPointerException.class, () -> useCase().execute(null));
    }

    @Test
    void execute_shouldDeleteSkippedResolutionAndReturnObligationId() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);
        OccurrenceResolution skipped = existing(obligation, d(2026, 8, 13), ResolutionStatus.SKIPPED);
        OccurrenceResolution other = existing(obligation, d(2026, 8, 6), ResolutionStatus.SKIPPED);

        UUID result = useCase().execute(command(obligation, d(2026, 8, 13)));

        assertEquals(obligation.id(), result);
        assertTrue(transactionManager.executed);
        assertEquals(List.of(skipped.id()), resolutionRepository.deleted);
        assertEquals(List.of(other), resolutionRepository.resolutions);
    }

    @Test
    void execute_shouldThrowWhenOccurrenceIsPaid() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);
        existing(obligation, d(2026, 8, 13), ResolutionStatus.PAID);

        assertRule(BusinessRuleCode.OCCURRENCE_PAID_NOT_REOPENABLE,
            "Paid occurrence can only be reopened by cancelling its expense",
            () -> useCase().execute(command(obligation, d(2026, 8, 13))));

        assertEquals(0, resolutionRepository.deleted.size());
        assertEquals(1, resolutionRepository.resolutions.size());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenOccurrenceHasNoResolution() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(command(obligation, d(2026, 8, 13))));

        assertEquals("Occurrence resolution not found for obligation " + obligation.id() + " on 2026-08-13",
            e.getMessage());
        assertEquals(0, resolutionRepository.deleted.size());
    }

    @Test
    void execute_shouldThrowWhenObligationIsArchivedBeforeLookingUpTheResolution() {
        Obligation obligation = weekly(ObligationStatus.ARCHIVED);
        existing(obligation, d(2026, 8, 13), ResolutionStatus.SKIPPED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> useCase().execute(command(obligation, d(2026, 8, 13))));

        assertEquals(0, resolutionRepository.findByDateCalls);
        assertEquals(0, resolutionRepository.deleted.size());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenObligationDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(new ReopenOccurrenceCommand(userId, missingId, d(2026, 8, 13))));

        assertEquals("Obligation not found for user: " + missingId, e.getMessage());
        assertEquals(0, resolutionRepository.findByDateCalls);
        assertEquals(0, resolutionRepository.deleted.size());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenObligationBelongsToAnotherUser() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);
        existing(obligation, d(2026, 8, 13), ResolutionStatus.SKIPPED);

        assertThrows(ResourceNotFoundException.class, () -> useCase()
            .execute(new ReopenOccurrenceCommand(UUID.randomUUID(), obligation.id(), d(2026, 8, 13))));

        assertEquals(0, resolutionRepository.findByDateCalls);
        assertEquals(0, resolutionRepository.deleted.size());
        assertEquals(1, resolutionRepository.resolutions.size());
    }

    @Test
    void execute_shouldReopenSkippedResolutionOffTheCurrentCalendar() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);
        OccurrenceResolution offCalendar = existing(obligation, d(2026, 8, 28), ResolutionStatus.SKIPPED);

        useCase().execute(command(obligation, d(2026, 8, 28)));

        assertEquals(List.of(offCalendar.id()), resolutionRepository.deleted);
    }

    @Test
    void execute_shouldMakeAPastDateOverdueAgain() {
        Obligation obligation = weekly(ObligationStatus.ACTIVE);
        existing(obligation, d(2026, 8, 6), ResolutionStatus.SKIPPED);
        existing(obligation, d(2026, 8, 13), ResolutionStatus.SKIPPED);
        assertEquals(0, obligation.overdueCount(TODAY, resolutionRepository.resolutions));

        useCase().execute(command(obligation, d(2026, 8, 13)));

        assertEquals(1, obligation.overdueCount(TODAY, resolutionRepository.resolutions));
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
