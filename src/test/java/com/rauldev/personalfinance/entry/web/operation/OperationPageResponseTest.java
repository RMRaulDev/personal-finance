package com.rauldev.personalfinance.entry.web.operation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationHistoryItem;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;

class OperationPageResponseTest {

    private static final OperationResponse ITEM = OperationResponse.from(new FinancialOperationHistoryItem(
        UUID.fromString("20000000-0000-4000-8000-000000000001"), OperationType.EXPENSE, Money.ofCents(100),
        LocalDate.of(2026, 10, 4), OperationStatus.ACTIVE, null,
        new AccountSummary(UUID.fromString("10000000-0000-4000-8000-000000000001"), "Wallet"),
        new CategorySummary(UUID.fromString("30000000-0000-4000-8000-000000000001"), "Food"), null));

    @Test
    void keepsItemsPageAndPageSize() {
        OperationPageResponse response = new OperationPageResponse(List.of(ITEM), 2, 25);

        assertEquals(List.of(ITEM), response.items());
        assertEquals(2, response.page());
        assertEquals(25, response.pageSize());
    }

    @Test
    void copiesItemsDefensively() {
        List<OperationResponse> source = new ArrayList<>(List.of(ITEM));
        OperationPageResponse response = new OperationPageResponse(source, 1, 20);

        source.clear();

        assertEquals(List.of(ITEM), response.items());
    }

    @Test
    void itemsAreUnmodifiable() {
        OperationPageResponse response = new OperationPageResponse(new ArrayList<>(List.of(ITEM)), 1, 20);

        assertThrows(UnsupportedOperationException.class, () -> response.items().add(ITEM));
    }

    @Test
    void rejectsNullItems() {
        assertThrows(NullPointerException.class, () -> new OperationPageResponse(null, 1, 20));
    }

    @Test
    void rejectsListContainingNullItem() {
        List<OperationResponse> withNull = new ArrayList<>();
        withNull.add(null);

        assertThrows(NullPointerException.class, () -> new OperationPageResponse(withNull, 1, 20));
    }
}
