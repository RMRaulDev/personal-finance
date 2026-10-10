package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.port.out.ObligationQueryPort;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.application.readmodel.ObligationOccurrence;
import com.rauldev.personalfinance.application.readmodel.ObligationOverview;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.domain.ResolutionSnapshot;

class ListObligationsTest {
    private static final Clock CLOCK = Clock.fixed(
        LocalDate.of(2026, 10, 10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();

    @Test
    void execute_shouldReturnAnOverviewPerObligationAndPassTheUserId() {
        UUID userId = UUID.randomUUID();
        List<ObligationDetails> expected = List.of(details("Gym", ObligationStatus.ARCHIVED),
            details("Rent", ObligationStatus.ACTIVE));
        RecordingObligationQueryPort port = new RecordingObligationQueryPort(expected);

        List<ObligationOverview> result = new ListObligations(port, transactionManager, CLOCK)
            .execute(new ListObligationsQuery(userId));

        assertEquals(List.of(new ObligationOverview(expected.get(0), null, 0),
            new ObligationOverview(expected.get(1), LocalDate.of(2026, 10, 20), 0)), result);
        assertEquals(1, transactionManager.executions);
        assertEquals(1, port.listCalls);
        assertEquals(userId, port.lastUserId);
        assertEquals(0, port.findCalls);
    }

    @Test
    void execute_shouldReturnEmptyListWhenPortReturnsEmpty() {
        RecordingObligationQueryPort port = new RecordingObligationQueryPort(List.of());

        List<ObligationOverview> result = new ListObligations(port, transactionManager, CLOCK)
            .execute(new ListObligationsQuery(UUID.randomUUID()));

        assertEquals(List.of(), result);
        assertEquals(1, port.listCalls);
    }

    @Test
    void execute_shouldRejectNullQuery() {
        RecordingObligationQueryPort port = new RecordingObligationQueryPort(List.of());
        ListObligations listObligations = new ListObligations(port, transactionManager, CLOCK);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> listObligations.execute(null));

        assertEquals("Query cannot be null", ex.getMessage());
        assertEquals(0, port.listCalls);
    }

    @Test
    void query_shouldRejectNullUserId() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new ListObligationsQuery(null));

        assertEquals("User id cannot be null", ex.getMessage());
    }

    @Test
    void constructor_shouldRejectNullObligationQueryPort() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new ListObligations(null, transactionManager, CLOCK));

        assertEquals("Obligation query port cannot be null", ex.getMessage());
    }

    static ObligationDetails details(String name, ObligationStatus status) {
        return new ObligationDetails(UUID.randomUUID(), name, Money.ofCents(2500),
            new AccountSummary(UUID.randomUUID(), "Wallet"), new CategorySummary(UUID.randomUUID(), "Rent"),
            new Recurrence(Frequency.MONTHLY, LocalDate.of(2026, 10, 20), null), status,
            status == ObligationStatus.ARCHIVED ? LocalDate.of(2026, 10, 1) : null);
    }

    private static final class RecordingTransactionManager implements TransactionManager {
        private int executions;

        @Override
        public <T> T execute(Supplier<T> transactionalWork) {
            executions++;
            return transactionalWork.get();
        }
    }

    private static final class RecordingObligationQueryPort implements ObligationQueryPort {
        private final List<ObligationDetails> response;
        private int listCalls;
        private int findCalls;
        private UUID lastUserId;

        private RecordingObligationQueryPort(List<ObligationDetails> response) {
            this.response = response;
        }

        @Override
        public List<ObligationDetails> findByUserId(UUID userId) {
            listCalls++;
            lastUserId = userId;
            return response;
        }

        @Override
        public Optional<ObligationDetails> findByIdAndUserId(UUID obligationId, UUID userId) {
            findCalls++;
            return Optional.empty();
        }

        @Override
        public List<ResolutionSnapshot> findResolutionsByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public List<ObligationOccurrence> findResolvedOccurrences(UUID obligationId, UUID userId) {
            return List.of();
        }
    }
}
