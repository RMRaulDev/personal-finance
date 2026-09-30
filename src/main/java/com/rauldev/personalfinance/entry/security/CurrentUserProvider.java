package com.rauldev.personalfinance.entry.security;

import java.util.UUID;

/**
 * Boundary that resolves the identity of the authenticated user for the current request.
 *
 * <p>Not implemented yet: no authentication mechanism has been chosen. There is intentionally no
 * implementation, no fixed or default user, and no bean that depends on this interface. Controllers
 * that need the user identity must wait for an implementation instead of accepting a user id from
 * the request.
 */
public interface CurrentUserProvider {

    UUID currentUserId();
}
