package com.rauldev.personalfinance.application;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.FinancialOperationRepository;
import com.rauldev.personalfinance.application.port.out.ReversalRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.query.OperationSearchCriteria;
import com.rauldev.personalfinance.application.query.OperationType;

class ApplicationContractsTest {
    @Test
    void criteriaRequiresUserAndAcceptsIndependentDateFilters() {
        UUID userId = UUID.randomUUID();
        OperationSearchCriteria criteria = new OperationSearchCriteria(
            userId, null, null, OperationType.INCOME,
            LocalDate.of(2026, 8, 20), null);

        assertEquals(userId, criteria.userId());
        assertEquals(OperationType.INCOME, criteria.operationType());
        assertNotNull(criteria.fromDate());
        assertNotNull(assertThrows(NullPointerException.class,
            () -> OperationSearchCriteria.forUser(null)));
    }

    @Test
    void criteriaRejectsInvertedDateRange() {
        assertNotNull(assertThrows(IllegalArgumentException.class, () -> new OperationSearchCriteria(
            UUID.randomUUID(), null, null, null,
            LocalDate.of(2026, 8, 21), LocalDate.of(2026, 8, 20))));
    }

    @Test
    void criteriaAcceptsEqualDateRange() {
        LocalDate date = LocalDate.of(2026, 8, 20);

        OperationSearchCriteria criteria = new OperationSearchCriteria(
            UUID.randomUUID(), null, null, null, date, date);

        assertEquals(date, criteria.fromDate());
        assertEquals(date, criteria.toDate());
    }

    @Test
    void transactionManagerSupportsResultAndVoidWork() {
        TransactionManager manager = new ImmediateTransactionManager();

        assertEquals("done", manager.execute(() -> "done"));
        manager.execute(() -> { });
    }

    @Test
    void repositoryContractsAreInterfaces() {
        assertTrue(AccountRepository.class.isInterface());
        assertTrue(CategoryRepository.class.isInterface());
        assertTrue(FinancialOperationRepository.class.isInterface());
        assertTrue(ReversalRepository.class.isInterface());
        assertTrue(TransactionManager.class.isInterface());
    }

    private static final class ImmediateTransactionManager implements TransactionManager {
        @Override
        public <T> T execute(java.util.function.Supplier<T> work) {
            return work.get();
        }
    }
}
