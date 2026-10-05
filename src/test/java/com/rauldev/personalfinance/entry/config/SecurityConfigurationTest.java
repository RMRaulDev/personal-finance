package com.rauldev.personalfinance.entry.config;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.rauldev.personalfinance.application.port.out.UserQueryPort;
import com.rauldev.personalfinance.entry.security.ConfiguredSingleUserProvider;
import com.rauldev.personalfinance.entry.security.CurrentUserProvider;

class SecurityConfigurationTest {

    private static final String PROPERTY = "personal-finance.single-user-id=";
    private static final String VALID = "123e4567-e89b-42d3-a456-426614174000";

    @Test
    void failsStartupWhenUserIdIsMissing() {
        RecordingUserQueryPort port = new RecordingUserQueryPort();

        runner(port).run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(messages(context.getStartupFailure()).contains("Single user id is required"));
        });
    }

    @Test
    void failsStartupWhenUserIdIsBlank() {
        RecordingUserQueryPort port = new RecordingUserQueryPort();

        runner(port).withPropertyValues(PROPERTY + "   ").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(messages(context.getStartupFailure()).contains("Single user id is required"));
        });
    }

    @Test
    void failsStartupWhenUserIdIsNotAUuid() {
        RecordingUserQueryPort port = new RecordingUserQueryPort();

        runner(port).withPropertyValues(PROPERTY + "not-a-uuid").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(messages(context.getStartupFailure()).contains("Single user id must be a valid UUID"));
        });
    }

    @Test
    void failsStartupWhenUserIdIsLenientUuid() {
        RecordingUserQueryPort port = new RecordingUserQueryPort();

        runner(port).withPropertyValues(PROPERTY + "1-2-3-4-5").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(messages(context.getStartupFailure()).contains("Single user id must be a valid UUID"));
        });
    }

    @Test
    void exposesConfiguredSingleUserProviderWithoutCheckingTheDatabase() {
        RecordingUserQueryPort port = new RecordingUserQueryPort();

        runner(port).withPropertyValues(PROPERTY + VALID).run(context -> {
            assertNull(context.getStartupFailure());
            CurrentUserProvider provider = context.getBean(CurrentUserProvider.class);
            assertInstanceOf(ConfiguredSingleUserProvider.class, provider);
            assertEquals(0, port.existsCalls);
        });
    }

    @Test
    void providerResolvesTheConfiguredUserWhenItExists() {
        RecordingUserQueryPort port = new RecordingUserQueryPort();
        port.exists = true;

        runner(port).withPropertyValues(PROPERTY + VALID.toUpperCase()).run(context -> {
            assertEquals(UUID.fromString(VALID), context.getBean(CurrentUserProvider.class).currentUserId());
            assertEquals(UUID.fromString(VALID), port.lastUserId);
        });
    }

    private static ApplicationContextRunner runner(UserQueryPort port) {
        return new ApplicationContextRunner()
            .withBean(UserQueryPort.class, () -> port)
            .withUserConfiguration(SecurityConfiguration.class);
    }

    private static String messages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }

    private static final class RecordingUserQueryPort implements UserQueryPort {
        boolean exists;
        int existsCalls;
        UUID lastUserId;

        @Override
        public boolean existsById(UUID userId) {
            existsCalls++;
            lastUserId = userId;
            return exists;
        }
    }
}
