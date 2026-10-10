package com.rauldev.personalfinance.application.usecase;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryType;

/**
 * Creates the user's first account, with its opening balance, and the default categories, in one
 * transaction. It only runs for a user without accounts and without categories (any status), which
 * also makes a retry after a timeout fail with {@code INITIAL_SETUP_ALREADY_COMPLETED} instead of
 * creating anything twice.
 */
public final class CompleteInitialSetup {
    /**
     * Default categories, created ACTIVE in this order. The list is a product decision, not a
     * domain rule.
     */
    static final List<DefaultCategory> DEFAULT_CATEGORIES = List.of(
        new DefaultCategory("Alimentación", CategoryType.EXPENSE),
        new DefaultCategory("Transporte", CategoryType.EXPENSE),
        new DefaultCategory("Vivienda", CategoryType.EXPENSE),
        new DefaultCategory("Servicios", CategoryType.EXPENSE),
        new DefaultCategory("Salud", CategoryType.EXPENSE),
        new DefaultCategory("Educación", CategoryType.EXPENSE),
        new DefaultCategory("Entretenimiento", CategoryType.EXPENSE),
        new DefaultCategory("Compras", CategoryType.EXPENSE),
        new DefaultCategory("Otros", CategoryType.EXPENSE),
        new DefaultCategory("Salario", CategoryType.INCOME),
        new DefaultCategory("Freelance", CategoryType.INCOME),
        new DefaultCategory("Negocio", CategoryType.INCOME),
        new DefaultCategory("Inversiones", CategoryType.INCOME),
        new DefaultCategory("Otros ingresos", CategoryType.INCOME));

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionManager transactionManager;

    public CompleteInitialSetup(
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        TransactionManager transactionManager
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "Category repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }

    /**
     * @return the id of the created account
     */
    public UUID execute(CompleteInitialSetupCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            if (!accountRepository.findByUserId(command.userId()).isEmpty()
                || !categoryRepository.findByUserId(command.userId()).isEmpty()) {
                throw new BusinessRuleViolationException(BusinessRuleCode.INITIAL_SETUP_ALREADY_COMPLETED,
                    "Initial setup is already completed");
            }

            Account account = new Account(command.userId(), command.accountName(), command.openingBalance());
            accountRepository.create(account);

            for (DefaultCategory defaultCategory : DEFAULT_CATEGORIES) {
                categoryRepository.create(
                    new Category(command.userId(), defaultCategory.name(), defaultCategory.type()));
            }

            return account.id();
        });
    }

    record DefaultCategory(String name, CategoryType type) {
        DefaultCategory {
            Objects.requireNonNull(name, "Category name cannot be null");
            Objects.requireNonNull(type, "Category type cannot be null");
        }
    }
}
