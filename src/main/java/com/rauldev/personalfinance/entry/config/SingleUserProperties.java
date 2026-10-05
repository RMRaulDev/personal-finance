package com.rauldev.personalfinance.entry.config;

import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Identity of the only user in single-user mode.
 *
 * <p>Binds {@code personal-finance.single-user-id} (for example, from the
 * {@code PERSONAL_FINANCE_SINGLE_USER_ID} environment variable). Because this is a record with a
 * single constructor, Spring Boot uses constructor binding: it passes the property value to the
 * compact constructor, so an invalid value fails startup instead of producing a half-valid bean.
 *
 * <p>The id is required and has no default: there is no fixed or fallback user.
 *
 * <p>Sharing the {@code personal-finance} prefix with other properties records is intentional:
 * each record binds only its own components.
 */
@ConfigurationProperties(prefix = "personal-finance")
public record SingleUserProperties(String singleUserId) {

    public SingleUserProperties {
        if (singleUserId == null || singleUserId.isBlank()) {
            throw new IllegalArgumentException(
                "Single user id is required. Set 'personal-finance.single-user-id' "
                    + "(for example, via the PERSONAL_FINANCE_SINGLE_USER_ID environment variable)");
        }
        if (!isCanonicalUuid(singleUserId)) {
            throw new IllegalArgumentException("Single user id must be a valid UUID");
        }
    }

    /**
     * @return the configured user id; {@link UUID#toString()} gives the lowercase form stored in
     *     {@code users.id}
     */
    public UUID userId() {
        return UUID.fromString(singleUserId);
    }

    /**
     * {@link UUID#fromString(String)} also accepts non-canonical forms such as {@code 1-2-3-4-5};
     * only the canonical 36-character form (any letter case) is accepted here.
     */
    private static boolean isCanonicalUuid(String value) {
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
