package com.rauldev.personalfinance.entry.web.common;

/**
 * Presence checks for request fields.
 *
 * <p>Commands and read models reject {@code null} with {@code NullPointerException}, which the
 * error handler answers as 500. Request records check their required fields with this helper
 * before building a command, so a missing JSON field answers 400. Value rules (positive amounts,
 * name length, ...) stay in the Core.
 */
public final class RequiredFields {

    private RequiredFields() {
    }

    /**
     * @return the value, if present
     * @throws IllegalArgumentException if the value is {@code null}
     */
    public static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Field '" + field + "' is required");
        }
        return value;
    }
}
