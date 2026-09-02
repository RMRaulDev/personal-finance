package com.rauldev.personalfinance.infrastructure.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;

import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.infrastructure.persistence.SQLiteConnectionProvider;

public final class JdbcTransactionManager implements TransactionManager {
    private final SQLiteConnectionProvider connectionProvider;
    private final TransactionConnectionHolder connectionHolder;

    public JdbcTransactionManager(
        SQLiteConnectionProvider connectionProvider,
        TransactionConnectionHolder connectionHolder
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "Connection provider cannot be null");
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    public JdbcTransactionManager(SQLiteConnectionProvider connectionProvider) {
        this(connectionProvider, new TransactionConnectionHolder());
    }

    public TransactionConnectionHolder getConnectionHolder() {
        return connectionHolder;
    }

    @Override
    public <T> T execute(Supplier<T> transactionalWork) {
        Objects.requireNonNull(transactionalWork, "Transactional work cannot be null");

        if (connectionHolder.hasActiveTransaction()) {
            throw new IllegalStateException("Nested transactions are not supported");
        }

        Connection connection;
        try {
            connection = connectionProvider.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open database connection for transaction", e);
        }

        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException closeEx) {
                e.addSuppressed(closeEx);
            }
            throw new RuntimeException("Failed to disable auto-commit for transaction", e);
        }

        connectionHolder.bind(connection);

        T result = null;
        Throwable primaryException = null;

        try {
            result = transactionalWork.get();
        } catch (Throwable t) {
            primaryException = t;
        }

        if (primaryException != null) {
            try {
                connection.rollback();
            } catch (Throwable rollbackEx) {
                primaryException.addSuppressed(rollbackEx);
            } finally {
                connectionHolder.clear();
                try {
                    connection.close();
                } catch (Throwable closeEx) {
                    primaryException.addSuppressed(closeEx);
                }
            }
            if (primaryException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (primaryException instanceof Error error) {
                throw error;
            } else {
                throw new RuntimeException("Transactional work threw checked exception", primaryException);
            }
        }

        try {
            connection.commit();
        } catch (Throwable commitEx) {
            primaryException = commitEx;
            try {
                connection.rollback();
            } catch (Throwable rollbackEx) {
                primaryException.addSuppressed(rollbackEx);
            }
        } finally {
            connectionHolder.clear();
            try {
                connection.close();
            } catch (Throwable closeEx) {
                if (primaryException != null) {
                    primaryException.addSuppressed(closeEx);
                } else {
                    primaryException = closeEx;
                }
            }
        }

        if (primaryException != null) {
            if (primaryException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (primaryException instanceof Error error) {
                throw error;
            } else {
                throw new RuntimeException("Transaction commit failed", primaryException);
            }
        }

        return result;
    }
}
