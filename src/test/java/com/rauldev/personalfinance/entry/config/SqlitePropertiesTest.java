package com.rauldev.personalfinance.entry.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SqlitePropertiesTest {

    @Test
    void acceptsValidJdbcSqliteUrl() {
        SqliteProperties properties = new SqliteProperties("jdbc:sqlite:/tmp/finance.db");

        assertEquals("jdbc:sqlite:/tmp/finance.db", properties.url());
    }

    @Test
    void acceptsInMemoryJdbcSqliteUrl() {
        SqliteProperties properties = new SqliteProperties("jdbc:sqlite::memory:");

        assertEquals("jdbc:sqlite::memory:", properties.url());
    }

    @Test
    void rejectsNullUrl() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new SqliteProperties(null));

        assertTrue(ex.getMessage().contains("SQLite URL is required"));
    }

    @Test
    void rejectsEmptyUrl() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new SqliteProperties(""));

        assertTrue(ex.getMessage().contains("SQLite URL is required"));
    }

    @Test
    void rejectsBlankUrl() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new SqliteProperties("   "));

        assertTrue(ex.getMessage().contains("SQLite URL is required"));
    }

    @Test
    void rejectsUrlWithWrongPrefix() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new SqliteProperties("jdbc:postgresql://localhost/finance"));

        assertTrue(ex.getMessage().contains("SQLite URL must start with 'jdbc:sqlite:'"));
    }

    @Test
    void rejectsPlainFilePath() {
        assertThrows(IllegalArgumentException.class, () -> new SqliteProperties("/tmp/finance.db"));
    }
}
