package com.rauldev.personalfinance.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Commitment of one ACTIVE account: the committed amount of the ACTIVE obligations paid from it whose account and
 * category are active, compared with its balance.
 */
public record AccountCommitment(UUID accountId, Money committed, Money balance, Money shortfall) {

    public AccountCommitment {
        Objects.requireNonNull(accountId, "Account id cannot be null");
        Objects.requireNonNull(committed, "Committed amount cannot be null");
        Objects.requireNonNull(balance, "Balance cannot be null");
        Objects.requireNonNull(shortfall, "Shortfall cannot be null");
    }
}
