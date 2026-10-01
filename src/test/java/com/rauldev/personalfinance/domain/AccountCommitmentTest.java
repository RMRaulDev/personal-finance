package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class AccountCommitmentTest {
    private static final Money ZERO = Money.ofCents(0);

    @Test
    void rejectsNullFields() {
        UUID id = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> new AccountCommitment(null, ZERO, ZERO, ZERO));
        assertThrows(NullPointerException.class, () -> new AccountCommitment(id, null, ZERO, ZERO));
        assertThrows(NullPointerException.class, () -> new AccountCommitment(id, ZERO, null, ZERO));
        assertThrows(NullPointerException.class, () -> new AccountCommitment(id, ZERO, ZERO, null));
    }
}
