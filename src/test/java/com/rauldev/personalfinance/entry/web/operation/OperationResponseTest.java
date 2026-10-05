package com.rauldev.personalfinance.entry.web.operation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationDetails;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationHistoryItem;
import com.rauldev.personalfinance.application.readmodel.TransferDetails;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;

class OperationResponseTest {

    private static final UUID ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final AccountSummary WALLET =
        new AccountSummary(UUID.fromString("10000000-0000-4000-8000-000000000001"), "Wallet");
    private static final AccountSummary SAVINGS =
        new AccountSummary(UUID.fromString("10000000-0000-4000-8000-000000000002"), "Savings");
    private static final CategorySummary FOOD =
        new CategorySummary(UUID.fromString("30000000-0000-4000-8000-000000000001"), "Food");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 4);

    @Test
    void mapsActiveExpenseHistoryItemWithCentsAndNullCancelledAtAndTransfer() {
        FinancialOperationHistoryItem item = new FinancialOperationHistoryItem(
            ID, OperationType.EXPENSE, Money.of("25.99"), DATE, OperationStatus.ACTIVE, null, WALLET, FOOD, null);

        OperationResponse response = OperationResponse.from(item);

        assertEquals(ID, response.id());
        assertEquals("EXPENSE", response.type());
        assertEquals(2599L, response.amountCents());
        assertEquals(DATE, response.operationDate());
        assertEquals("ACTIVE", response.status());
        assertNull(response.cancelledAt());
        assertEquals("Wallet", response.account().name());
        assertEquals("Food", response.category().name());
        assertNull(response.transfer());
    }

    @Test
    void mapsCancelledIncomeDetailsWithCancelledAt() {
        Instant cancelledAt = Instant.parse("2026-10-05T15:30:00Z");
        FinancialOperationDetails details = new FinancialOperationDetails(
            ID, OperationType.INCOME, Money.ofCents(1), DATE, OperationStatus.CANCELLED, cancelledAt, WALLET, FOOD,
            null);

        OperationResponse response = OperationResponse.from(details);

        assertEquals("INCOME", response.type());
        assertEquals(1L, response.amountCents());
        assertEquals("CANCELLED", response.status());
        assertEquals(cancelledAt, response.cancelledAt());
    }

    @Test
    void mapsTransferHistoryItemWithNullStatusAccountAndCategory() {
        FinancialOperationHistoryItem item = new FinancialOperationHistoryItem(
            ID, OperationType.TRANSFER, Money.of("0.50"), DATE, null, null, null, null,
            new TransferDetails(WALLET, SAVINGS));

        OperationResponse response = OperationResponse.from(item);

        assertEquals("TRANSFER", response.type());
        assertEquals(50L, response.amountCents());
        assertNull(response.status());
        assertNull(response.cancelledAt());
        assertNull(response.account());
        assertNull(response.category());
        assertEquals("Wallet", response.transfer().sourceAccount().name());
        assertEquals(SAVINGS.id(), response.transfer().targetAccount().id());
    }

    @Test
    void mapsTransferDetailsWithNullStatusAccountAndCategory() {
        FinancialOperationDetails details = new FinancialOperationDetails(
            ID, OperationType.TRANSFER, Money.ofCents(0), DATE, null, null, null, null,
            new TransferDetails(WALLET, SAVINGS));

        OperationResponse response = OperationResponse.from(details);

        assertEquals(0L, response.amountCents());
        assertNull(response.status());
        assertNull(response.account());
        assertNull(response.category());
    }

    @Test
    void rejectsNullHistoryItem() {
        assertThrows(NullPointerException.class, () -> OperationResponse.from((FinancialOperationHistoryItem) null));
    }

    @Test
    void rejectsNullDetails() {
        assertThrows(NullPointerException.class, () -> OperationResponse.from((FinancialOperationDetails) null));
    }
}
