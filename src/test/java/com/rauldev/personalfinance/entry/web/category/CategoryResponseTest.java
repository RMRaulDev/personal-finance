package com.rauldev.personalfinance.entry.web.category;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.readmodel.CategoryDetails;
import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.CategoryType;

class CategoryResponseTest {

    private static final UUID ID = UUID.fromString("20000000-0000-4000-8000-000000000001");

    @Test
    void mapsFieldsAndEnumsToNames() {
        CategoryDetails details = new CategoryDetails(ID, "Food", CategoryType.EXPENSE, CategoryStatus.INACTIVE);

        assertEquals(new CategoryResponse(ID, "Food", "EXPENSE", "INACTIVE"), CategoryResponse.from(details));
    }

    @Test
    void mapsIncomeActiveCategory() {
        CategoryDetails details = new CategoryDetails(ID, "Salary", CategoryType.INCOME, CategoryStatus.ACTIVE);

        assertEquals(new CategoryResponse(ID, "Salary", "INCOME", "ACTIVE"), CategoryResponse.from(details));
    }

    @Test
    void rejectsNullDetails() {
        assertThrows(NullPointerException.class, () -> CategoryResponse.from(null));
    }
}
