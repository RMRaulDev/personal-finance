package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;

public final class ModifyAccount {
    private final AccountRepository accountRepository;
    private final TransactionManager transactionManager;

    public ModifyAccount(
        AccountRepository accountRepository,
        TransactionManager transactionManager
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }

    public UUID execute(ModifyAccountCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            Account account = accountRepository.findByIdAndUserId(command.accountId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Account not found for user: " + command.accountId()));

            if (accountRepository.existsByUserIdAndNameAndIdNot(command.userId(), command.name(), command.accountId())) {
                throw new IllegalArgumentException("An account with the same name already exists for this user");
            }

            account.rename(command.name());
            accountRepository.update(account);
            return account.id();
        });
    }
}
