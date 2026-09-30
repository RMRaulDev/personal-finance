package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.SQLException;

import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

final class SqliteConstraintViolations {

    private SqliteConstraintViolations() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * Returns whether the exception is a SQLite UNIQUE violation of the constraint over the given
     * columns. SQLite names the columns in its message ("UNIQUE constraint failed: table.col, table.col"),
     * which is the only way to tell apart different UNIQUE constraints on the same table.
     */
    static boolean isUniqueViolation(SQLException exception, String columns) {
        return exception instanceof SQLiteException sqliteException
            && sqliteException.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE
            && sqliteException.getMessage().contains(columns);
    }
}
