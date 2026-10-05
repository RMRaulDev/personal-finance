package com.rauldev.personalfinance.entry.config;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SingleUserPropertiesTest {

    private static final String VALID = "123e4567-e89b-42d3-a456-426614174000";

    @Test
    void acceptsCanonicalUuid() {
        SingleUserProperties properties = new SingleUserProperties(VALID);

        assertEquals(UUID.fromString(VALID), properties.userId());
    }

    @Test
    void acceptsUppercaseUuidAndExposesItLowercase() {
        SingleUserProperties properties = new SingleUserProperties(VALID.toUpperCase());

        assertEquals(VALID, properties.userId().toString());
    }

    @Test
    void rejectsNullId() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new SingleUserProperties(null));

        assertTrue(ex.getMessage().contains("Single user id is required"));
    }

    @Test
    void rejectsBlankId() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new SingleUserProperties("   "));

        assertTrue(ex.getMessage().contains("Single user id is required"));
    }

    @Test
    void rejectsEmptyId() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new SingleUserProperties(""));

        assertTrue(ex.getMessage().contains("Single user id is required"));
    }

    @Test
    void rejectsNonUuidId() {
        IllegalArgumentException ex =
            assertThrows(IllegalArgumentException.class, () -> new SingleUserProperties("not-a-uuid"));

        assertEquals("Single user id must be a valid UUID", ex.getMessage());
    }

    @Test
    void rejectsLenientNonCanonicalUuid() {
        IllegalArgumentException ex =
            assertThrows(IllegalArgumentException.class, () -> new SingleUserProperties("1-2-3-4-5"));

        assertEquals("Single user id must be a valid UUID", ex.getMessage());
    }

    @Test
    void rejectsUuidWithSurroundingWhitespace() {
        IllegalArgumentException ex =
            assertThrows(IllegalArgumentException.class, () -> new SingleUserProperties(" " + VALID));

        assertEquals("Single user id must be a valid UUID", ex.getMessage());
    }
}
