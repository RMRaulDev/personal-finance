package com.rauldev.personalfinance.entry.config;

import java.time.DateTimeException;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Time zone used to decide what "today" is for the user.
 *
 * <p>Binds {@code personal-finance.time-zone} (for example, from the
 * {@code PERSONAL_FINANCE_TIME_ZONE} environment variable). {@link DefaultValue} supplies the
 * value when the property is absent; this only works with constructor binding, which Spring Boot
 * uses for records. An invalid zone id fails startup.
 *
 * <p>Sharing the {@code personal-finance} prefix with other properties records is intentional:
 * each record binds only its own components.
 */
@ConfigurationProperties(prefix = "personal-finance")
public record TimeProperties(@DefaultValue("America/Mexico_City") String timeZone) {

    public TimeProperties {
        if (timeZone == null) {
            throw new IllegalArgumentException(
                "Time zone is required. Set 'personal-finance.time-zone' to a zone id such as America/Mexico_City");
        }
        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(
                "Invalid time zone '" + timeZone + "'. Set 'personal-finance.time-zone' "
                    + "to a zone id such as America/Mexico_City", e);
        }
    }

    public ZoneId zoneId() {
        return ZoneId.of(timeZone);
    }
}
