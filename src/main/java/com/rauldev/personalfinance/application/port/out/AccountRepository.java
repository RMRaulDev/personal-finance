package com.rauldev.personalfinance.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Account;

public interface AccountRepository {
    Optional<Account> findById(UUID id);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    void save(Account account);
}
