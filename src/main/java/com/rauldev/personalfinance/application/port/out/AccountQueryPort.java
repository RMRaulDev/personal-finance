package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.AccountDetails;

public interface AccountQueryPort {
    Optional<AccountDetails> findByIdAndUserId(UUID accountId, UUID userId);

    /**
     * Returns all the user's accounts, active and inactive, ordered by name and then by id.
     * Returns an empty list when the user has no accounts.
     */
    List<AccountDetails> findByUserId(UUID userId);
}
