package com.rauldev.personalfinance.entry.security;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.port.out.UserQueryPort;

class ConfiguredSingleUserProviderTest {

    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-42d3-a456-426614174000");

    @Test
    void returnsConfiguredUserIdWhenUserExists() {
        RecordingUserQueryPort port = new RecordingUserQueryPort(true);
        ConfiguredSingleUserProvider provider = new ConfiguredSingleUserProvider(USER_ID, port);

        assertEquals(USER_ID, provider.currentUserId());
        assertEquals(USER_ID, port.lastUserId);
    }

    @Test
    void doesNotCheckTheDatabaseUntilFirstUse() {
        RecordingUserQueryPort port = new RecordingUserQueryPort(true);

        new ConfiguredSingleUserProvider(USER_ID, port);

        assertEquals(0, port.existsCalls);
    }

    @Test
    void cachesPositiveResultAfterFirstCheck() {
        RecordingUserQueryPort port = new RecordingUserQueryPort(true);
        ConfiguredSingleUserProvider provider = new ConfiguredSingleUserProvider(USER_ID, port);

        provider.currentUserId();
        provider.currentUserId();
        provider.currentUserId();

        assertEquals(1, port.existsCalls);
    }

    @Test
    void throwsSingleUserNotProvisionedWhenUserDoesNotExist() {
        RecordingUserQueryPort port = new RecordingUserQueryPort(false);
        ConfiguredSingleUserProvider provider = new ConfiguredSingleUserProvider(USER_ID, port);

        SingleUserNotProvisionedException ex =
            assertThrows(SingleUserNotProvisionedException.class, provider::currentUserId);

        assertEquals("Configured single user " + USER_ID + " does not exist; insert it in lowercase into the users table",
            ex.getMessage());
    }

    @Test
    void notProvisionedExceptionIsAnIllegalStateException() {
        RecordingUserQueryPort port = new RecordingUserQueryPort(false);
        ConfiguredSingleUserProvider provider = new ConfiguredSingleUserProvider(USER_ID, port);

        assertThrows(IllegalStateException.class, provider::currentUserId);
    }

    @Test
    void checksAgainAfterNegativeResultSoLateProvisioningNeedsNoRestart() {
        RecordingUserQueryPort port = new RecordingUserQueryPort(false);
        ConfiguredSingleUserProvider provider = new ConfiguredSingleUserProvider(USER_ID, port);
        assertThrows(SingleUserNotProvisionedException.class, provider::currentUserId);
        assertThrows(SingleUserNotProvisionedException.class, provider::currentUserId);
        assertEquals(2, port.existsCalls);

        port.exists = true;

        assertEquals(USER_ID, provider.currentUserId());
        assertEquals(3, port.existsCalls);
    }

    @Test
    void rejectsNullUserId() {
        NullPointerException ex = assertThrows(NullPointerException.class,
            () -> new ConfiguredSingleUserProvider(null, new RecordingUserQueryPort(true)));

        assertEquals("User id cannot be null", ex.getMessage());
    }

    @Test
    void rejectsNullUserQueryPort() {
        NullPointerException ex = assertThrows(NullPointerException.class,
            () -> new ConfiguredSingleUserProvider(USER_ID, null));

        assertEquals("User query port cannot be null", ex.getMessage());
    }

    private static final class RecordingUserQueryPort implements UserQueryPort {
        boolean exists;
        int existsCalls;
        UUID lastUserId;

        RecordingUserQueryPort(boolean exists) {
            this.exists = exists;
        }

        @Override
        public boolean existsById(UUID userId) {
            existsCalls++;
            lastUserId = userId;
            return exists;
        }
    }
}
