package com.rauldev.personalfinance.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Category;

public final class CreateCategory {
    private final CategoryRepository categoryRepository;
    private final TransactionManager transactionManager;

    public CreateCategory(
        CategoryRepository categoryRepository,
        TransactionManager transactionManager
    ) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "Category repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }

    public UUID execute(CreateCategoryCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            if (categoryRepository.existsByUserIdAndName(command.userId(), command.name())) {
                throw new IllegalArgumentException("A category with the same name already exists for this user");
            }

            Category category = new Category(command.userId(), command.name(), command.type());
            categoryRepository.create(category);
            return category.id();
        });
    }
}
