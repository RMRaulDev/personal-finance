package com.rauldev.personalfinance.infrastructure.persistence;

/**
 * Thrown when a persisted row cannot be mapped back to a domain object or read model because its
 * stored values are invalid (malformed UUID, unknown enum value, broken invariant, ...).
 *
 * <p>It signals a server-side data problem, not a client error, so it deliberately does not extend
 * {@link IllegalArgumentException}. The message identifies the source and row for diagnostics and
 * must not be exposed to clients.
 */
public final class CorruptedPersistedDataException extends RuntimeException {
    public CorruptedPersistedDataException(String source, String rowId, Throwable cause) {
        super("Corrupted persisted data in " + source + " row '" + rowId + "'", cause);
    }
}
