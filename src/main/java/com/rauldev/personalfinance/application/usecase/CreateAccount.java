package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;

public final class CreateAccount {
    private final AccountRepository accountRepository;
    private final TransactionManager transactionManager;

    public CreateAccount(
        AccountRepository accountRepository,
        TransactionManager transactionManager
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }

    public UUID execute(CreateAccountCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            if (accountRepository.existsByUserIdAndName(command.userId(), command.name())) {
                throw new IllegalArgumentException("An account with the same name already exists for this user");
            }

            Account account = new Account(command.userId(), command.name());
            accountRepository.create(account);
            return account.id();
        });
    }
}
