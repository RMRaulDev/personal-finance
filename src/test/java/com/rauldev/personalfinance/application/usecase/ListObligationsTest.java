package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.port.out.ObligationQueryPort;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;

class ListObligationsTest {
    @Test
    void execute_shouldReturnTheListFromThePortUnchangedAndPassTheUserId() {
        UUID userId = UUID.randomUUID();
        List<ObligationDetails> expected = List.of(details("Gym", ObligationStatus.ARCHIVED),
            details("Rent", ObligationStatus.ACTIVE));
        RecordingObligationQueryPort port = new RecordingObligationQueryPort(expected);

        List<ObligationDetails> result = new ListObligations(port).execute(new ListObligationsQuery(userId));

        assertSame(expected, result);
        assertEquals(1, port.listCalls);
        assertEquals(userId, port.lastUserId);
        assertEquals(0, port.findCalls);
    }

    @Test
    void execute_shouldReturnEmptyListWhenPortReturnsEmpty() {
        RecordingObligationQueryPort port = new RecordingObligationQueryPort(List.of());

        List<ObligationDetails> result = new ListObligations(port).execute(new ListObligationsQuery(UUID.randomUUID()));

        assertEquals(List.of(), result);
        assertEquals(1, port.listCalls);
    }

    @Test
    void execute_shouldRejectNullQuery() {
        RecordingObligationQueryPort port = new RecordingObligationQueryPort(List.of());
        ListObligations listObligations = new ListObligations(port);

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
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new ListObligations(null));

        assertEquals("Obligation query port cannot be null", ex.getMessage());
    }

    static ObligationDetails details(String name, ObligationStatus status) {
        return new ObligationDetails(UUID.randomUUID(), name, Money.ofCents(2500),
            new AccountSummary(UUID.randomUUID(), "Wallet"), new CategorySummary(UUID.randomUUID(), "Rent"),
            new Recurrence(Frequency.MONTHLY, LocalDate.of(2026, 10, 20), null), status);
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
    }
}
