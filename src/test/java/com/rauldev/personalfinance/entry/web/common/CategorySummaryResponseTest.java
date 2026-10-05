package com.rauldev.personalfinance.entry.web.common;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.readmodel.CategorySummary;

class CategorySummaryResponseTest {

    private static final UUID ID = UUID.fromString("123e4567-e89b-42d3-a456-426614174000");

    @Test
    void mapsIdAndName() {
        CategorySummaryResponse response = CategorySummaryResponse.from(new CategorySummary(ID, "Food"));

        assertEquals(new CategorySummaryResponse(ID, "Food"), response);
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> CategorySummaryResponse.from(null));
    }
}
