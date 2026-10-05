package com.rauldev.personalfinance.application.usecase;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.port.out.CategoryQueryPort;
import com.rauldev.personalfinance.application.readmodel.CategoryDetails;
import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.CategoryType;

class ListCategoriesTest {
    @Test
    void execute_shouldReturnTheListFromThePortUnchangedAndPassTheUserId() {
        UUID userId = UUID.randomUUID();
        List<CategoryDetails> expected = List.of(
            new CategoryDetails(UUID.randomUUID(), "Food", CategoryType.EXPENSE, CategoryStatus.ACTIVE),
            new CategoryDetails(UUID.randomUUID(), "Salary", CategoryType.INCOME, CategoryStatus.INACTIVE));
        RecordingCategoryQueryPort port = new RecordingCategoryQueryPort(expected);

        List<CategoryDetails> result = new ListCategories(port).execute(new ListCategoriesQuery(userId));

        assertSame(expected, result);
        assertEquals(1, port.listCalls);
        assertEquals(userId, port.lastUserId);
    }

    @Test
    void execute_shouldReturnEmptyListWhenPortReturnsEmpty() {
        RecordingCategoryQueryPort port = new RecordingCategoryQueryPort(List.of());

        List<CategoryDetails> result = new ListCategories(port).execute(new ListCategoriesQuery(UUID.randomUUID()));

        assertEquals(List.of(), result);
        assertEquals(1, port.listCalls);
    }

    @Test
    void execute_shouldRejectNullQuery() {
        RecordingCategoryQueryPort port = new RecordingCategoryQueryPort(List.of());
        ListCategories listCategories = new ListCategories(port);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> listCategories.execute(null));

        assertEquals("Query cannot be null", ex.getMessage());
        assertEquals(0, port.listCalls);
    }

    @Test
    void query_shouldRejectNullUserId() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new ListCategoriesQuery(null));

        assertEquals("User id cannot be null", ex.getMessage());
    }

    @Test
    void constructor_shouldRejectNullCategoryQueryPort() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new ListCategories(null));

        assertEquals("Category query port cannot be null", ex.getMessage());
    }

    private static final class RecordingCategoryQueryPort implements CategoryQueryPort {
        private final List<CategoryDetails> response;
        private int listCalls;
        private UUID lastUserId;

        private RecordingCategoryQueryPort(List<CategoryDetails> response) {
            this.response = response;
        }

        @Override
        public List<CategoryDetails> findByUserId(UUID userId) {
            listCalls++;
            lastUserId = userId;
            return response;
        }
    }
}
