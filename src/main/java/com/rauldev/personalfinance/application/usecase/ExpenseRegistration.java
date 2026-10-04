package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.Expense;

/**
 * Registers an expense shared by {@link RegisterExpense} and {@link PayOccurrence}. It opens no transaction of its
 * own: callers must run it inside {@code TransactionManager.execute}, so it joins their transaction.
 */
public final class ExpenseRegistration {
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseOperationRepository expenseOperationRepository;

    public ExpenseRegistration(
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        ExpenseOperationRepository expenseOperationRepository
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "Category repository cannot be null");
        this.expenseOperationRepository = Objects.requireNonNull(expenseOperationRepository,
            "Expense operation repository cannot be null");
    }

    /**
     * Loads the account and category by user, registers the expense, debits the account, and saves both.
     *
     * @return the registered expense
     */
    public Expense register(RegisterExpenseCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

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

        return expense;
    }
}
