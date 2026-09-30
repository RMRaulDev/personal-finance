package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;

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
                throw new BusinessRuleViolationException(BusinessRuleCode.ACCOUNT_NAME_ALREADY_EXISTS,
                    ApplicationConstants.ACCOUNT_NAME_ALREADY_EXISTS_MESSAGE);
            }

            Account account = new Account(command.userId(), command.name());
            accountRepository.create(account);
            return account.id();
        });
    }
}
