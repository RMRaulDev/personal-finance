package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.port.out.TransferOperationRepository;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Transfer;

public final class RegisterTransfer {
    private final AccountRepository accountRepository;
    private final TransferOperationRepository transferOperationRepository;
    private final TransactionManager transactionManager;

    public RegisterTransfer(
        AccountRepository accountRepository,
        TransferOperationRepository transferOperationRepository,
        TransactionManager transactionManager
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository cannot be null");
        this.transferOperationRepository = Objects.requireNonNull(transferOperationRepository,
            "Transfer operation repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }

    public UUID execute(RegisterTransferCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            Account sourceAccount = accountRepository.findByIdAndUserId(command.sourceAccountId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Source account not found for user: " + command.sourceAccountId()));

            Account targetAccount = accountRepository.findByIdAndUserId(command.targetAccountId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Target account not found for user: " + command.targetAccountId()));

            Transfer transfer = Transfer.register(
                sourceAccount,
                targetAccount,
                command.amount(),
                command.operationDate()
            );

            sourceAccount.debit(command.amount());
            targetAccount.credit(command.amount());

            transferOperationRepository.create(transfer);
            accountRepository.update(sourceAccount);
            accountRepository.update(targetAccount);

            return transfer.id();
        });
    }
}
