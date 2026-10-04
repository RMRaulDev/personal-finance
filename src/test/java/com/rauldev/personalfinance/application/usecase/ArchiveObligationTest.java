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

class ArchiveObligationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final UUID userId = UUID.randomUUID();
    private final RecordingObligationRepository obligationRepository = new RecordingObligationRepository();
    private final RecordingResolutionRepository resolutionRepository = new RecordingResolutionRepository();
    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();

    private ArchiveObligation useCase() {
        return new ArchiveObligation(obligationRepository, resolutionRepository, transactionManager, CLOCK);
    }

    private Obligation obligation(LocalDate start, ObligationStatus status) {
        Obligation obligation = new Obligation(UUID.randomUUID(), userId, "Rent", Money.ofCents(100),
            UUID.randomUUID(), UUID.randomUUID(), new Recurrence(Frequency.MONTHLY, start, null), status);
        obligationRepository.stored.add(obligation);
        return obligation;
    }

    @Test
    void constructor_shouldThrowWhenObligationRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ArchiveObligation(null, resolutionRepository, transactionManager, CLOCK));
        assertEquals("Obligation repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenOccurrenceResolutionRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ArchiveObligation(obligationRepository, null, transactionManager, CLOCK));
        assertEquals("Occurrence resolution repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenTransactionManagerIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ArchiveObligation(obligationRepository, resolutionRepository, null, CLOCK));
        assertEquals("Transaction manager cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenClockIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ArchiveObligation(obligationRepository, resolutionRepository, transactionManager, null));
        assertEquals("Clock cannot be null", e.getMessage());
    }

    @Test
    void execute_shouldThrowWhenCommandIsNull() {
        assertThrows(NullPointerException.class, () -> useCase().execute(null));
    }

    @Test
    void execute_shouldArchiveObligationAndPersistIt() {
        Obligation obligation = obligation(TODAY.plusDays(10), ObligationStatus.ACTIVE);

        UUID result = useCase().execute(new ArchiveObligationCommand(userId, obligation.id()));

        assertEquals(obligation.id(), result);
        assertTrue(transactionManager.executed);
        assertEquals(1, obligationRepository.updateCalls);
        assertEquals(obligation, obligationRepository.updatedObligation);
        assertEquals(ObligationStatus.ARCHIVED, obligationRepository.updatedObligation.status());
    }

    @Test
    void execute_shouldArchiveObligationWhenItsOnlyDueDateIsToday() {
        Obligation obligation = obligation(TODAY, ObligationStatus.ACTIVE);

        useCase().execute(new ArchiveObligationCommand(userId, obligation.id()));

        assertEquals(ObligationStatus.ARCHIVED, obligation.status());
        assertEquals(1, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenObligationDoesNotExist() {
        assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(new ArchiveObligationCommand(userId, UUID.randomUUID())));

        assertEquals(0, resolutionRepository.findCalls);
        assertEquals(0, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenObligationBelongsToAnotherUser() {
        Obligation obligation = obligation(TODAY.plusDays(10), ObligationStatus.ACTIVE);

        assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(new ArchiveObligationCommand(UUID.randomUUID(), obligation.id())));

        assertEquals(ObligationStatus.ACTIVE, obligation.status());
        assertEquals(0, resolutionRepository.findCalls);
        assertEquals(0, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowWhenObligationHasOverdueOccurrences() {
        Obligation obligation = obligation(TODAY.minusDays(1), ObligationStatus.ACTIVE);

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> useCase().execute(new ArchiveObligationCommand(userId, obligation.id())));

        assertEquals(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES, e.code());
        assertEquals("Obligation has overdue occurrences", e.getMessage());
        assertEquals(ObligationStatus.ACTIVE, obligation.status());
        assertEquals(0, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldArchiveWhenOverdueOccurrencesAreResolved() {
        Obligation obligation = obligation(TODAY.minusMonths(1), ObligationStatus.ACTIVE);
        resolutionRepository.resolutions.add(skipped(obligation, TODAY.minusMonths(1)));

        useCase().execute(new ArchiveObligationCommand(userId, obligation.id()));

        assertEquals(1, resolutionRepository.findCalls);
        assertEquals(ObligationStatus.ARCHIVED, obligation.status());
        assertEquals(1, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowWhenOneOfSeveralOverdueOccurrencesIsUnresolved() {
        Obligation obligation = obligation(TODAY.minusMonths(2), ObligationStatus.ACTIVE);
        resolutionRepository.resolutions.add(skipped(obligation, TODAY.minusMonths(2)));

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> useCase().execute(new ArchiveObligationCommand(userId, obligation.id())));

        assertEquals(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES, e.code());
        assertEquals(ObligationStatus.ACTIVE, obligation.status());
        assertEquals(0, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowWhenObligationIsAlreadyArchived() {
        Obligation obligation = obligation(TODAY.plusDays(10), ObligationStatus.ARCHIVED);

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> useCase().execute(new ArchiveObligationCommand(userId, obligation.id())));

        assertEquals(BusinessRuleCode.OBLIGATION_ARCHIVED, e.code());
        assertEquals("Obligation is archived", e.getMessage());
        assertEquals(0, obligationRepository.updateCalls);
    }

    private static OccurrenceResolution skipped(Obligation obligation, LocalDate dueDate) {
        return new OccurrenceResolution(UUID.randomUUID(), obligation.id(), dueDate, ResolutionStatus.SKIPPED, null,
            Instant.parse("2026-08-01T00:00:00Z"));
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
        private int updateCalls;
        private Obligation updatedObligation;

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
            updateCalls++;
            updatedObligation = obligation;
            return obligation;
        }
    }

    private static final class RecordingResolutionRepository implements OccurrenceResolutionRepository {
        private final List<OccurrenceResolution> resolutions = new ArrayList<>();
        private int findCalls;

        @Override
        public OccurrenceResolution create(OccurrenceResolution resolution) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OccurrenceResolution> findByObligationId(UUID obligationId) {
            findCalls++;
            return resolutions.stream().filter(r -> r.obligationId().equals(obligationId)).toList();
        }

        @Override
        public Optional<OccurrenceResolution> findByObligationIdAndDueDate(UUID obligationId, LocalDate dueDate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OccurrenceResolution> findByExpenseId(UUID expenseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(UUID resolutionId) {
            throw new UnsupportedOperationException();
        }
    }
}
