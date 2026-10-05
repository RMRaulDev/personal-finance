package com.rauldev.personalfinance.entry.security;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.UserQueryPort;

/**
 * Single-user mode: every request belongs to the user configured in
 * {@code personal-finance.single-user-id}.
 *
 * <p>The user's existence is checked on first use, not at startup, so creating the bean never
 * opens a database connection. Only a positive result is cached: if the user is missing, every
 * call checks again, so inserting the user later needs no restart. The bean is a singleton shared
 * by all request threads, hence the {@code volatile} flag; two threads may both run the check
 * once, which is harmless.
 *
 * <p>Built by {@code entry.config.SecurityConfiguration}; it carries no Spring annotations.
 */
public final class ConfiguredSingleUserProvider implements CurrentUserProvider {
    private final UUID userId;
    private final UserQueryPort userQueryPort;
    private volatile boolean userVerified;

    public ConfiguredSingleUserProvider(UUID userId, UserQueryPort userQueryPort) {
        this.userId = Objects.requireNonNull(userId, "User id cannot be null");
        this.userQueryPort = Objects.requireNonNull(userQueryPort, "User query port cannot be null");
    }

    /**
     * @throws SingleUserNotProvisionedException if the configured user does not exist
     */
    @Override
    public UUID currentUserId() {
        if (!userVerified) {
            if (!userQueryPort.existsById(userId)) {
                throw new SingleUserNotProvisionedException(userId);
            }
            userVerified = true;
        }
        return userId;
    }
}
