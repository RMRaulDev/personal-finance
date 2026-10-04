package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;

public final class RegisterExpense {
    private final ExpenseRegistration expenseRegistration;
    private final TransactionManager transactionManager;

    public RegisterExpense(
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        ExpenseOperationRepository expenseOperationRepository,
        TransactionManager transactionManager
    ) {
        this.expenseRegistration = new ExpenseRegistration(
            accountRepository,
            categoryRepository,
            expenseOperationRepository
        );
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }

    public UUID execute(RegisterExpenseCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> expenseRegistration.register(command).id());
    }
}
