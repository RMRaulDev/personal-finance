package com.rauldev.personalfinance.entry.config;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ClockConfigurationTest {

    private final ApplicationContextRunner runner =
        new ApplicationContextRunner().withUserConfiguration(ClockConfiguration.class);

    @Test
    void usesMexicoCityZoneByDefault() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(ZoneId.of("America/Mexico_City"), context.getBean(Clock.class).getZone());
        });
    }

    @Test
    void usesConfiguredZone() {
        runner.withPropertyValues("personal-finance.time-zone=Asia/Tokyo").run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(ZoneId.of("Asia/Tokyo"), context.getBean(Clock.class).getZone());
        });
    }

    @Test
    void failsStartupWhenZoneIsInvalid() {
        runner.withPropertyValues("personal-finance.time-zone=Mars/Olympus").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(messages(context.getStartupFailure()).contains("Invalid time zone 'Mars/Olympus'"));
        });
    }

    private static String messages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }
}
