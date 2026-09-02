package com.rauldev.personalfinance.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.FinancialOperationQueryPort;
import com.rauldev.personalfinance.application.query.OperationSearchCriteria;
import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationDetails;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationHistoryItem;
import com.rauldev.personalfinance.application.readmodel.TransferDetails;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;

class GetOperationDetailsTest {
    @Test
    void execute_shouldReturnOperationDetailsWhenOperationExists() {
        UUID userId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        FinancialOperationDetails expected = createIncomeDetails(operationId, userId);

        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(Optional.of(expected));
        GetOperationDetails getOperationDetails = new GetOperationDetails(queryPort);

        FinancialOperationDetails result = getOperationDetails.execute(new GetOperationDetailsQuery(userId, operationId));

        assertSame(expected, result);
        assertEquals(1, queryPort.findCalls);
        assertEquals(operationId, queryPort.lastOperationId);
        assertEquals(userId, queryPort.lastUserId);
    }

    @Test
    void execute_shouldDelegateOperationIdAndUserIdExactlyOnce() {
        UUID userId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        FinancialOperationDetails expected = createExpenseDetails(operationId, userId);

        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(Optional.of(expected));
        GetOperationDetails getOperationDetails = new GetOperationDetails(queryPort);

        getOperationDetails.execute(new GetOperationDetailsQuery(userId, operationId));

        assertEquals(1, queryPort.findCalls);
        assertEquals(operationId, queryPort.lastOperationId);
        assertEquals(userId, queryPort.lastUserId);
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenOperationDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(Optional.empty());
        GetOperationDetails getOperationDetails = new GetOperationDetails(queryPort);

        assertThrows(ResourceNotFoundException.class,
            () -> getOperationDetails.execute(new GetOperationDetailsQuery(userId, operationId)));

        assertEquals(1, queryPort.findCalls);
        assertEquals(operationId, queryPort.lastOperationId);
        assertEquals(userId, queryPort.lastUserId);
    }

    @Test
    void execute_shouldUseUserScopeWhenQueryingOperationDetails() {
        UUID userId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        FinancialOperationDetails expected = createTransferDetails(operationId);

        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(Optional.of(expected));
        GetOperationDetails getOperationDetails = new GetOperationDetails(queryPort);

        getOperationDetails.execute(new GetOperationDetailsQuery(userId, operationId));

        assertEquals(userId, queryPort.lastUserId);
        assertEquals(operationId, queryPort.lastOperationId);
    }

    @Test
    void execute_shouldReturnCancelledOperationDetailsUnchanged() {
        UUID userId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        FinancialOperationDetails expected = createCancelledExpenseDetails(operationId, userId);

        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(Optional.of(expected));
        GetOperationDetails getOperationDetails = new GetOperationDetails(queryPort);

        FinancialOperationDetails result = getOperationDetails.execute(new GetOperationDetailsQuery(userId, operationId));

        assertSame(expected, result);
        assertEquals(OperationStatus.CANCELLED, result.status());
        assertEquals(Instant.parse("2026-08-25T10:30:00Z"), result.cancelledAt());
    }

    @Test
    void execute_shouldReturnTransferOperationDetailsUnchanged() {
        UUID userId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        FinancialOperationDetails expected = createTransferDetails(operationId);

        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(Optional.of(expected));
        GetOperationDetails getOperationDetails = new GetOperationDetails(queryPort);

        FinancialOperationDetails result = getOperationDetails.execute(new GetOperationDetailsQuery(userId, operationId));

        assertSame(expected, result);
        assertEquals(OperationType.TRANSFER, result.operationType());
        assertEquals(expected.transfer(), result.transfer());
    }

    @Test
    void execute_shouldRejectNullQuery() {
        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(Optional.empty());
        GetOperationDetails getOperationDetails = new GetOperationDetails(queryPort);

        assertThrows(NullPointerException.class, () -> getOperationDetails.execute(null));
        assertEquals(0, queryPort.findCalls);
    }

    @Test
    void query_shouldRejectNullUserId() {
        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(Optional.empty());
        GetOperationDetails getOperationDetails = new GetOperationDetails(queryPort);

        assertThrows(NullPointerException.class,
            () -> getOperationDetails.execute(new GetOperationDetailsQuery(null, UUID.randomUUID())));
        assertEquals(0, queryPort.findCalls);
    }

    @Test
    void query_shouldRejectNullOperationId() {
        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(Optional.empty());
        GetOperationDetails getOperationDetails = new GetOperationDetails(queryPort);

        assertThrows(NullPointerException.class,
            () -> getOperationDetails.execute(new GetOperationDetailsQuery(UUID.randomUUID(), null)));
        assertEquals(0, queryPort.findCalls);
    }

    @Test
    void constructor_shouldRejectNullQueryPort() {
        assertThrows(NullPointerException.class, () -> new GetOperationDetails(null));
    }

    private static FinancialOperationDetails createIncomeDetails(UUID operationId, UUID userId) {
        AccountSummary account = new AccountSummary(UUID.randomUUID(), "Checking");
        CategorySummary category = new CategorySummary(UUID.randomUUID(), "Salary");
        return new FinancialOperationDetails(
            operationId,
            OperationType.INCOME,
            Money.ofCents(2500),
            LocalDate.of(2026, 8, 24),
            OperationStatus.ACTIVE,
            null,
            account,
            category,
            null
        );
    }

    private static FinancialOperationDetails createExpenseDetails(UUID operationId, UUID userId) {
        AccountSummary account = new AccountSummary(UUID.randomUUID(), "Card");
        CategorySummary category = new CategorySummary(UUID.randomUUID(), "Groceries");
        return new FinancialOperationDetails(
            operationId,
            OperationType.EXPENSE,
            Money.ofCents(890),
            LocalDate.of(2026, 8, 25),
            OperationStatus.ACTIVE,
            null,
            account,
            category,
            null
        );
    }

    private static FinancialOperationDetails createCancelledExpenseDetails(UUID operationId, UUID userId) {
        AccountSummary account = new AccountSummary(UUID.randomUUID(), "Card");
        CategorySummary category = new CategorySummary(UUID.randomUUID(), "Groceries");
        return new FinancialOperationDetails(
            operationId,
            OperationType.EXPENSE,
            Money.ofCents(890),
            LocalDate.of(2026, 8, 25),
            OperationStatus.CANCELLED,
            Instant.parse("2026-08-25T10:30:00Z"),
            account,
            category,
            null
        );
    }

    private static FinancialOperationDetails createTransferDetails(UUID operationId) {
        AccountSummary source = new AccountSummary(UUID.randomUUID(), "Checking");
        AccountSummary target = new AccountSummary(UUID.randomUUID(), "Savings");
        TransferDetails transfer = new TransferDetails(source, target);

        return new FinancialOperationDetails(
            operationId,
            OperationType.TRANSFER,
            Money.ofCents(1500),
            LocalDate.of(2026, 8, 26),
            null,
            null,
            null,
            null,
            transfer
        );
    }

    private static final class RecordingFinancialOperationQueryPort implements FinancialOperationQueryPort {
        private final Optional<FinancialOperationDetails> response;
        private int findCalls;
        private UUID lastOperationId;
        private UUID lastUserId;

        private RecordingFinancialOperationQueryPort(Optional<FinancialOperationDetails> response) {
            this.response = response;
        }

        @Override
        public java.util.List<FinancialOperationHistoryItem> search(OperationSearchCriteria criteria) {
            return java.util.List.of();
        }

        @Override
        public Optional<FinancialOperationDetails> findDetailByIdAndUserId(UUID operationId, UUID userId) {
            findCalls++;
            lastOperationId = operationId;
            lastUserId = userId;
            return response;
        }
    }
}
