package com.rauldev.personalfinance.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.port.out.AccountQueryPort;
import com.rauldev.personalfinance.application.readmodel.AccountDetails;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.Money;

class ListAccountsTest {
    @Test
    void execute_shouldReturnTheListFromThePortUnchangedAndPassTheUserId() {
        UUID userId = UUID.randomUUID();
        List<AccountDetails> expected = List.of(
            new AccountDetails(UUID.randomUUID(), userId, "Alpha", Money.ofCents(100), AccountStatus.ACTIVE),
            new AccountDetails(UUID.randomUUID(), userId, "Beta", Money.ofCents(0), AccountStatus.INACTIVE));
        RecordingAccountQueryPort port = new RecordingAccountQueryPort(expected);

        List<AccountDetails> result = new ListAccounts(port).execute(new ListAccountsQuery(userId));

        assertSame(expected, result);
        assertEquals(1, port.listCalls);
        assertEquals(userId, port.lastUserId);
    }

    @Test
    void execute_shouldReturnEmptyListWhenPortReturnsEmpty() {
        RecordingAccountQueryPort port = new RecordingAccountQueryPort(List.of());

        List<AccountDetails> result = new ListAccounts(port).execute(new ListAccountsQuery(UUID.randomUUID()));

        assertEquals(List.of(), result);
        assertEquals(1, port.listCalls);
    }

    @Test
    void execute_shouldRejectNullQuery() {
        RecordingAccountQueryPort port = new RecordingAccountQueryPort(List.of());
        ListAccounts listAccounts = new ListAccounts(port);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> listAccounts.execute(null));

        assertEquals("Query cannot be null", ex.getMessage());
        assertEquals(0, port.listCalls);
    }

    @Test
    void query_shouldRejectNullUserId() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new ListAccountsQuery(null));

        assertEquals("User id cannot be null", ex.getMessage());
    }

    @Test
    void constructor_shouldRejectNullAccountQueryPort() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new ListAccounts(null));

        assertEquals("Account query port cannot be null", ex.getMessage());
    }

    private static final class RecordingAccountQueryPort implements AccountQueryPort {
        private final List<AccountDetails> response;
        private int listCalls;
        private UUID lastUserId;

        private RecordingAccountQueryPort(List<AccountDetails> response) {
            this.response = response;
        }

        @Override
        public Optional<AccountDetails> findByIdAndUserId(UUID accountId, UUID userId) {
            throw new UnsupportedOperationException("Not used by ListAccounts");
        }

        @Override
        public List<AccountDetails> findByUserId(UUID userId) {
            listCalls++;
            lastUserId = userId;
            return response;
        }
    }
}
