package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class AccountSnapshotTest {
    private static final Money ZERO = Money.ofCents(0);

    @Test
    void rejectsNullFieldsWithTheirMessages() {
        UUID id = UUID.randomUUID();

        assertEquals("Account id cannot be null", assertThrows(NullPointerException.class,
            () -> new AccountSnapshot(null, "A", ZERO, AccountStatus.ACTIVE)).getMessage());
        assertEquals("Account name cannot be null", assertThrows(NullPointerException.class,
            () -> new AccountSnapshot(id, null, ZERO, AccountStatus.ACTIVE)).getMessage());
        assertEquals("Balance cannot be null", assertThrows(NullPointerException.class,
            () -> new AccountSnapshot(id, "A", null, AccountStatus.ACTIVE)).getMessage());
        assertEquals("Account status cannot be null", assertThrows(NullPointerException.class,
            () -> new AccountSnapshot(id, "A", ZERO, null)).getMessage());
    }
}
