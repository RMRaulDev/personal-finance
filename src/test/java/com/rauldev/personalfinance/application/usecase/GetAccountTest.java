package com.rauldev.personalfinance.application.usecase;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountQueryPort;
import com.rauldev.personalfinance.application.readmodel.AccountDetails;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.Money;

class GetAccountTest {
    @Test
    void execute_shouldReturnAccountDetailsWhenAccountExists() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AccountDetails expected = new AccountDetails(
            accountId,
            userId,
            "Checking",
            Money.ofCents(1250),
            AccountStatus.ACTIVE
        );

        RecordingAccountQueryPort accountQueryPort = new RecordingAccountQueryPort(Optional.of(expected));
        GetAccount getAccount = new GetAccount(accountQueryPort);

        AccountDetails result = getAccount.execute(new GetAccountQuery(userId, accountId));

        assertSame(expected, result);
        assertEquals(1, accountQueryPort.findCalls);
        assertEquals(accountId, accountQueryPort.lastAccountId);
        assertEquals(userId, accountQueryPort.lastUserId);
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenAccountDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        RecordingAccountQueryPort accountQueryPort = new RecordingAccountQueryPort(Optional.empty());
        GetAccount getAccount = new GetAccount(accountQueryPort);

        assertThrows(ResourceNotFoundException.class,
            () -> getAccount.execute(new GetAccountQuery(userId, accountId)));

        assertEquals(1, accountQueryPort.findCalls);
        assertEquals(accountId, accountQueryPort.lastAccountId);
        assertEquals(userId, accountQueryPort.lastUserId);
    }

    @Test
    void execute_shouldQueryByAccountIdAndUserIdTogether() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AccountDetails expected = new AccountDetails(
            accountId,
            userId,
            "Savings",
            Money.ofCents(3000),
            AccountStatus.INACTIVE
        );

        RecordingAccountQueryPort accountQueryPort = new RecordingAccountQueryPort(Optional.of(expected));
        GetAccount getAccount = new GetAccount(accountQueryPort);

        getAccount.execute(new GetAccountQuery(userId, accountId));

        assertEquals(accountId, accountQueryPort.lastAccountId);
        assertEquals(userId, accountQueryPort.lastUserId);
        assertEquals(1, accountQueryPort.findCalls);
    }

    @Test
    void execute_shouldRejectNullQuery() {
        RecordingAccountQueryPort accountQueryPort = new RecordingAccountQueryPort(Optional.empty());
        GetAccount getAccount = new GetAccount(accountQueryPort);

        assertThrows(NullPointerException.class, () -> getAccount.execute(null));
        assertEquals(0, accountQueryPort.findCalls);
    }

    @Test
    void query_shouldRejectNullUserId() {
        RecordingAccountQueryPort accountQueryPort = new RecordingAccountQueryPort(Optional.empty());
        GetAccount getAccount = new GetAccount(accountQueryPort);

        assertThrows(NullPointerException.class,
            () -> getAccount.execute(new GetAccountQuery(null, UUID.randomUUID())));
        assertEquals(0, accountQueryPort.findCalls);
    }

    @Test
    void query_shouldRejectNullAccountId() {
        RecordingAccountQueryPort accountQueryPort = new RecordingAccountQueryPort(Optional.empty());
        GetAccount getAccount = new GetAccount(accountQueryPort);

        assertThrows(NullPointerException.class,
            () -> getAccount.execute(new GetAccountQuery(UUID.randomUUID(), null)));
        assertEquals(0, accountQueryPort.findCalls);
    }

    @Test
    void constructor_shouldRejectNullAccountQueryPort() {
        assertThrows(NullPointerException.class, () -> new GetAccount(null));
    }

    private static final class RecordingAccountQueryPort implements AccountQueryPort {
        private final Optional<AccountDetails> response;
        private int findCalls;
        private UUID lastAccountId;
        private UUID lastUserId;

        private RecordingAccountQueryPort(Optional<AccountDetails> response) {
            this.response = response;
        }

        @Override
        public Optional<AccountDetails> findByIdAndUserId(UUID accountId, UUID userId) {
            findCalls++;
            lastAccountId = accountId;
            lastUserId = userId;
            return response;
        }
    }
}
