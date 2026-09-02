package com.rauldev.personalfinance.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.port.out.FinancialOperationQueryPort;
import com.rauldev.personalfinance.application.query.OperationSearchCriteria;
import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationDetails;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationHistoryItem;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;

class GetOperationHistoryTest {
    @Test
    void execute_shouldReturnOperationHistoryWhenQueryPortReturnsResults() {
        UUID userId = UUID.randomUUID();
        OperationSearchCriteria criteria = OperationSearchCriteria.forUser(userId);
        List<FinancialOperationHistoryItem> expected = List.of(createIncomeHistoryItem());

        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(expected);
        GetOperationHistory getOperationHistory = new GetOperationHistory(queryPort);

        List<FinancialOperationHistoryItem> result = getOperationHistory.execute(criteria);

        assertSame(expected, result);
        assertEquals(1, queryPort.searchCalls);
        assertSame(criteria, queryPort.lastCriteria);
    }

    @Test
    void execute_shouldReturnEmptyHistoryWhenQueryPortReturnsEmptyList() {
        UUID userId = UUID.randomUUID();
        OperationSearchCriteria criteria = OperationSearchCriteria.forUser(userId);

        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(List.of());
        GetOperationHistory getOperationHistory = new GetOperationHistory(queryPort);

        List<FinancialOperationHistoryItem> result = getOperationHistory.execute(criteria);

        assertEquals(List.of(), result);
        assertEquals(1, queryPort.searchCalls);
        assertSame(criteria, queryPort.lastCriteria);
    }

    @Test
    void execute_shouldDelegateCriteriaToQueryPortExactlyOnce() {
        UUID userId = UUID.randomUUID();
        OperationSearchCriteria criteria = OperationSearchCriteria.forUser(userId);

        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(List.of());
        GetOperationHistory getOperationHistory = new GetOperationHistory(queryPort);

        getOperationHistory.execute(criteria);

        assertEquals(1, queryPort.searchCalls);
        assertSame(criteria, queryPort.lastCriteria);
    }

    @Test
    void execute_shouldRejectNullCriteria() {
        RecordingFinancialOperationQueryPort queryPort = new RecordingFinancialOperationQueryPort(List.of());
        GetOperationHistory getOperationHistory = new GetOperationHistory(queryPort);

        assertThrows(NullPointerException.class, () -> getOperationHistory.execute(null));
        assertEquals(0, queryPort.searchCalls);
    }

    @Test
    void constructor_shouldRejectNullQueryPort() {
        assertThrows(NullPointerException.class, () -> new GetOperationHistory(null));
    }

    private static FinancialOperationHistoryItem createIncomeHistoryItem() {
        AccountSummary account = new AccountSummary(UUID.randomUUID(), "Checking");
        CategorySummary category = new CategorySummary(UUID.randomUUID(), "Salary");

        return new FinancialOperationHistoryItem(
            UUID.randomUUID(),
            OperationType.INCOME,
            Money.of(BigDecimal.valueOf(150.00)),
            LocalDate.of(2026, 8, 24),
            OperationStatus.ACTIVE,
            null,
            account,
            category,
            null
        );
    }

    private static final class RecordingFinancialOperationQueryPort implements FinancialOperationQueryPort {
        private final List<FinancialOperationHistoryItem> response;
        private int searchCalls;
        private OperationSearchCriteria lastCriteria;

        private RecordingFinancialOperationQueryPort(List<FinancialOperationHistoryItem> response) {
            this.response = response;
        }

        @Override
        public List<FinancialOperationHistoryItem> search(OperationSearchCriteria criteria) {
            searchCalls++;
            lastCriteria = criteria;
            return response;
        }

        @Override
        public Optional<FinancialOperationDetails> findDetailById(UUID operationId) {
            return Optional.empty();
        }
    }
}
