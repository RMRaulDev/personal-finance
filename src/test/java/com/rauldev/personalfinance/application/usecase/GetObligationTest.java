package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.ObligationQueryPort;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;

class GetObligationTest {
    @Test
    void execute_shouldReturnTheObligationFromThePortAndPassBothIds() {
        UUID userId = UUID.randomUUID();
        ObligationDetails expected = details("Rent", ObligationStatus.ACTIVE);
        RecordingObligationQueryPort port = new RecordingObligationQueryPort(Optional.of(expected));

        ObligationDetails result = new GetObligation(port)
            .execute(new GetObligationQuery(userId, expected.id()));

        assertSame(expected, result);
        assertEquals(1, port.findCalls);
        assertEquals(userId, port.lastUserId);
        assertEquals(expected.id(), port.lastObligationId);
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenObligationIsMissingOrForeign() {
        UUID obligationId = UUID.randomUUID();
        RecordingObligationQueryPort port = new RecordingObligationQueryPort(Optional.empty());
        GetObligation getObligation = new GetObligation(port);
        GetObligationQuery query = new GetObligationQuery(UUID.randomUUID(), obligationId);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
            () -> getObligation.execute(query));

        assertEquals("Obligation not found for user: " + obligationId, ex.getMessage());
        assertEquals(1, port.findCalls);
    }

    @Test
    void execute_shouldRejectNullQuery() {
        RecordingObligationQueryPort port = new RecordingObligationQueryPort(Optional.empty());
        GetObligation getObligation = new GetObligation(port);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> getObligation.execute(null));

        assertEquals("Query cannot be null", ex.getMessage());
        assertEquals(0, port.findCalls);
    }

    @Test
    void query_shouldRejectNullUserId() {
        UUID obligationId = UUID.randomUUID();

        NullPointerException ex = assertThrows(NullPointerException.class,
            () -> new GetObligationQuery(null, obligationId));

        assertEquals("User id cannot be null", ex.getMessage());
    }

    @Test
    void query_shouldRejectNullObligationId() {
        UUID userId = UUID.randomUUID();

        NullPointerException ex = assertThrows(NullPointerException.class,
            () -> new GetObligationQuery(userId, null));

        assertEquals("Obligation id cannot be null", ex.getMessage());
    }

    @Test
    void constructor_shouldRejectNullObligationQueryPort() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new GetObligation(null));

        assertEquals("Obligation query port cannot be null", ex.getMessage());
    }

    private static ObligationDetails details(String name, ObligationStatus status) {
        return new ObligationDetails(UUID.randomUUID(), name, Money.ofCents(2500),
            new AccountSummary(UUID.randomUUID(), "Wallet"), new CategorySummary(UUID.randomUUID(), "Rent"),
            new Recurrence(Frequency.MONTHLY, LocalDate.of(2026, 10, 20), null), status);
    }

    private static final class RecordingObligationQueryPort implements ObligationQueryPort {
        private final Optional<ObligationDetails> response;
        private int findCalls;
        private UUID lastUserId;
        private UUID lastObligationId;

        private RecordingObligationQueryPort(Optional<ObligationDetails> response) {
            this.response = response;
        }

        @Override
        public List<ObligationDetails> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public Optional<ObligationDetails> findByIdAndUserId(UUID obligationId, UUID userId) {
            findCalls++;
            lastObligationId = obligationId;
            lastUserId = userId;
            return response;
        }
    }
}
