package com.rauldev.personalfinance.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.AccountDetails;

public interface AccountQueryPort {
    Optional<AccountDetails> findByIdAndUserId(UUID accountId, UUID userId);
}
