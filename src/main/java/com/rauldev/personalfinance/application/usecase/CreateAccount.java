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
            boolean exists = accountRepository.existsByUserIdAndName(command.userId(), command.name());
            if (exists) {
                throw new IllegalArgumentException("Account with the same name already exists for the user");
            }

            Account account = new Account(command.userId(), command.name());
            return accountRepository.create(account).id();
        });
    }
}
