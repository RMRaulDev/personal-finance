package com.rauldev.personalfinance.infrastructure.transaction;

import java.sql.Connection;
import java.util.Objects;

public final class TransactionConnectionHolder {
    private static final ThreadLocal<Connection> CURRENT_CONNECTION = new ThreadLocal<>();

    public void bind(Connection connection) {
        Objects.requireNonNull(connection, "Connection cannot be null");
        CURRENT_CONNECTION.set(connection);
    }

    public Connection get() {
        Connection connection = CURRENT_CONNECTION.get();
        if (connection == null) {
            throw new IllegalStateException("No active transaction found for the current thread");
        }
        return connection;
    }

    public boolean hasActiveTransaction() {
        return CURRENT_CONNECTION.get() != null;
    }

    public void clear() {
        CURRENT_CONNECTION.remove();
    }
}
