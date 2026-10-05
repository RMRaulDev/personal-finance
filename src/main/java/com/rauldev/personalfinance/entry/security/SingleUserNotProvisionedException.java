package com.rauldev.personalfinance.entry.security;

import java.util.UUID;

/**
 * The configured single user does not exist in the database.
 *
 * <p>An operator configuration fault, not a client error: it is answered as a generic 500 and
 * logged with this message.
 */
public final class SingleUserNotProvisionedException extends IllegalStateException {

    public SingleUserNotProvisionedException(UUID userId) {
        super("Configured single user " + userId
            + " does not exist; insert it in lowercase into the users table");
    }
}
