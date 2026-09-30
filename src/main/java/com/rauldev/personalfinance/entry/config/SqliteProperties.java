package com.rauldev.personalfinance.entry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SQLite connection settings.
 *
 * <p>The URL is required and has no default: a missing URL must fail startup instead of letting
 * SQLite silently create a database file in an unexpected location.
 */
@ConfigurationProperties(prefix = "personal-finance.sqlite")
public record SqliteProperties(String url) {

    private static final String JDBC_SQLITE_PREFIX = "jdbc:sqlite:";

    public SqliteProperties {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                "SQLite URL is required. Set 'personal-finance.sqlite.url' "
                    + "(for example, via the PERSONAL_FINANCE_SQLITE_URL environment variable)");
        }
        if (!url.startsWith(JDBC_SQLITE_PREFIX)) {
            throw new IllegalArgumentException(
                "SQLite URL must start with '" + JDBC_SQLITE_PREFIX + "'");
        }
    }
}
