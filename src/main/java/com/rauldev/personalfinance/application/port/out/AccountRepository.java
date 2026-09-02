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

    default boolean existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID accountId) {
        if (!existsByUserIdAndName(userId, name)) {
            return false;
        }

        return findById(accountId)
            .map(account -> !account.userId().equals(userId) || !account.name().equals(name))
            .orElse(true);
    }

    Account update(Account account);
}
