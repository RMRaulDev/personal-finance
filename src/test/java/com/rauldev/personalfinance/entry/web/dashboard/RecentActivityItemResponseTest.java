package com.rauldev.personalfinance.entry.web.dashboard;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.ObligationSummary;
import com.rauldev.personalfinance.application.readmodel.RecentActivityItem;
import com.rauldev.personalfinance.application.readmodel.TransferDetails;
import com.rauldev.personalfinance.domain.Money;

class RecentActivityItemResponseTest {

    private static final UUID OPERATION_ID = UUID.fromString("50000000-0000-4000-8000-000000000001");
    private static final AccountSummary WALLET =
        new AccountSummary(UUID.fromString("10000000-0000-4000-8000-000000000001"), "Wallet");
    private static final AccountSummary SAVINGS =
        new AccountSummary(UUID.fromString("10000000-0000-4000-8000-000000000002"), "Savings");
    private static final CategorySummary CATEGORY =
        new CategorySummary(UUID.fromString("30000000-0000-4000-8000-000000000001"), "Salary");
    private static final ObligationSummary RENT =
        new ObligationSummary(UUID.fromString("40000000-0000-4000-8000-000000000001"), "Rent");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 4);

    @Test
    void mapsAnIncomeWithAccountAndCategoryAndNoTransferOrObligation() {
        RecentActivityItem item = new RecentActivityItem(OPERATION_ID, OperationType.INCOME, Money.ofCents(12345), DATE,
            WALLET, CATEGORY, null, null);

        RecentActivityItemResponse response = RecentActivityItemResponse.from(item);

        assertEquals(OPERATION_ID, response.id());
        assertEquals("INCOME", response.type());
        assertEquals(12345, response.amountCents());
        assertEquals(DATE, response.operationDate());
        assertEquals(WALLET.id(), response.account().id());
        assertEquals("Wallet", response.account().name());
        assertEquals(CATEGORY.id(), response.category().id());
        assertEquals("Salary", response.category().name());
        assertNull(response.transfer());
        assertNull(response.obligation());
    }

    @Test
    void mapsAnExpenseWithoutObligationWithANullObligation() {
        RecentActivityItem item = new RecentActivityItem(OPERATION_ID, OperationType.EXPENSE, Money.ofCents(500), DATE,
            WALLET, CATEGORY, null, null);

        RecentActivityItemResponse response = RecentActivityItemResponse.from(item);

        assertEquals("EXPENSE", response.type());
        assertNull(response.obligation());
    }

    @Test
    void mapsAnExpenseWithTheObligationItPaid() {
        RecentActivityItem item = new RecentActivityItem(OPERATION_ID, OperationType.EXPENSE, Money.ofCents(2500), DATE,
            WALLET, CATEGORY, null, RENT);

        RecentActivityItemResponse response = RecentActivityItemResponse.from(item);

        assertEquals(RENT.id(), response.obligation().id());
        assertEquals("Rent", response.obligation().name());
        assertEquals(2500, response.amountCents());
    }

    @Test
    void mapsATransferWithTransferDetailsAndNullAccountCategoryAndObligation() {
        RecentActivityItem item = new RecentActivityItem(OPERATION_ID, OperationType.TRANSFER, Money.ofCents(700), DATE,
            null, null, new TransferDetails(WALLET, SAVINGS), null);

        RecentActivityItemResponse response = RecentActivityItemResponse.from(item);

        assertEquals("TRANSFER", response.type());
        assertNull(response.account());
        assertNull(response.category());
        assertNull(response.obligation());
        assertEquals(WALLET.id(), response.transfer().sourceAccount().id());
        assertEquals(SAVINGS.id(), response.transfer().targetAccount().id());
    }

    @Test
    void rejectsNullItem() {
        assertThrows(NullPointerException.class, () -> RecentActivityItemResponse.from(null));
    }
}
