package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.IncomeOperationRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.Income;

public final class RegisterIncome {
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final IncomeOperationRepository incomeOperationRepository;
    private final TransactionManager transactionManager;

    public RegisterIncome(
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        IncomeOperationRepository incomeOperationRepository,
        TransactionManager transactionManager
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "Category repository cannot be null");
        this.incomeOperationRepository = Objects.requireNonNull(incomeOperationRepository,
            "Income operation repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }

    public UUID execute(RegisterIncomeCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            Account account = accountRepository.findByIdAndUserId(command.accountId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Account not found for user: " + command.accountId()));

            Category category = categoryRepository.findByIdAndUserId(command.categoryId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Category not found for user: " + command.categoryId()));

            Income income = Income.register(
                account,
                category,
                command.amount(),
                command.operationDate()
            );

            account.credit(command.amount());
            incomeOperationRepository.create(income);
            accountRepository.update(account);

            return income.id();
        });
    }
}
