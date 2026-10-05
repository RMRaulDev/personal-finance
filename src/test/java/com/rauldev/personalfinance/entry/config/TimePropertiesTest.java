package com.rauldev.personalfinance.entry.config;

import java.time.DateTimeException;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TimePropertiesTest {

    @Test
    void exposesValidZoneId() {
        TimeProperties properties = new TimeProperties("Europe/Madrid");

        assertEquals(ZoneId.of("Europe/Madrid"), properties.zoneId());
    }

    @Test
    void rejectsNullTimeZone() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TimeProperties(null));

        assertTrue(ex.getMessage().contains("Time zone is required"));
    }

    @Test
    void rejectsInvalidTimeZoneKeepingCause() {
        IllegalArgumentException ex =
            assertThrows(IllegalArgumentException.class, () -> new TimeProperties("Mars/Olympus"));

        assertTrue(ex.getMessage().contains("Invalid time zone 'Mars/Olympus'"));
        assertInstanceOf(DateTimeException.class, ex.getCause());
    }

    @Test
    void rejectsBlankTimeZone() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TimeProperties(" "));

        assertTrue(ex.getMessage().contains("Invalid time zone ' '"));
    }
}
