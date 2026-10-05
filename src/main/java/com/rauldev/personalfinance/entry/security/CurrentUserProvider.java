package com.rauldev.personalfinance.entry.security;

import java.util.UUID;

/**
 * Boundary that resolves the identity of the user for the current request.
 *
 * <p>Controllers obtain the user id only from this interface and never accept it from the
 * request (path, query, header, or body). No authentication mechanism exists yet, so the only
 * implementation is {@link ConfiguredSingleUserProvider} (single-user mode), wired in
 * {@code entry.config.SecurityConfiguration}. Do not add any other fixed, default, or fake user.
 * Real authentication will replace only that bean.
 */
public interface CurrentUserProvider {

    UUID currentUserId();
}
