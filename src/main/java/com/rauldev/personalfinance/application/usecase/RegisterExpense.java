package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.Expense;

public final class RegisterExpense {
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseOperationRepository expenseOperationRepository;
    private final TransactionManager transactionManager;

    public RegisterExpense(
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        ExpenseOperationRepository expenseOperationRepository,
        TransactionManager transactionManager
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "Category repository cannot be null");
        this.expenseOperationRepository = Objects.requireNonNull(expenseOperationRepository,
            "Expense operation repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }

    public UUID execute(RegisterExpenseCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            Account account = accountRepository.findByIdAndUserId(command.accountId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Account not found for user: " + command.accountId()));

            Category category = categoryRepository.findByIdAndUserId(command.categoryId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Category not found for user: " + command.categoryId()));

            Expense expense = Expense.register(
                account,
                category,
                command.amount(),
                command.operationDate()
            );

            account.debit(command.amount());
            expenseOperationRepository.create(expense);
            accountRepository.update(account);

            return expense.id();
        });
    }
}
