package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Account;

public interface AccountRepository {
    Account create(Account account);

    Optional<Account> findById(UUID id);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    List<Account> findByUserId(UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    Account update(Account account);
}
