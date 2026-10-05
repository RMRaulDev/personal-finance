package com.rauldev.personalfinance.entry.web.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class RequiredFieldsTest {

    @Test
    void returnsTheValueWhenPresent() {
        String value = "Checking";

        assertSame(value, RequiredFields.require(value, "name"));
    }

    @Test
    void returnsZeroAndEmptyValuesBecausePresenceIsTheOnlyRule() {
        assertEquals(0L, RequiredFields.require(0L, "amountCents"));
        assertEquals("", RequiredFields.require("", "name"));
    }

    @Test
    void rejectsNullNamingTheField() {
        IllegalArgumentException ex =
            assertThrows(IllegalArgumentException.class, () -> RequiredFields.require(null, "name"));

        assertEquals("Field 'name' is required", ex.getMessage());
    }
}
