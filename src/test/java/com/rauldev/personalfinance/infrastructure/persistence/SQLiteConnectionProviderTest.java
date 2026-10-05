package com.rauldev.personalfinance.infrastructure.persistence;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SQLiteConnectionProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void connectionsSetBusyTimeoutToFiveSeconds() throws Exception {
        SQLiteConnectionProvider provider = provider();

        assertEquals(5000, pragma(provider, "busy_timeout"));
    }

    @Test
    void connectionsEnableForeignKeys() throws Exception {
        SQLiteConnectionProvider provider = provider();

        assertEquals(1, pragma(provider, "foreign_keys"));
    }

    @Test
    void rejectsNullUrl() {
        assertThrows(NullPointerException.class, () -> new SQLiteConnectionProvider(null));
    }

    @Test
    void rejectsNonSqliteUrl() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new SQLiteConnectionProvider("jdbc:postgresql://localhost/x"));

        assertTrue(ex.getMessage().contains("jdbc:sqlite:"));
    }

    private SQLiteConnectionProvider provider() {
        return new SQLiteConnectionProvider("jdbc:sqlite:" + tempDir.resolve("pragmas.db").toAbsolutePath());
    }

    private static int pragma(SQLiteConnectionProvider provider, String name) throws Exception {
        try (Connection connection = provider.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA " + name)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }
}
