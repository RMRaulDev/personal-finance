package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.Obligation;

public final class CreateObligation {
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final ObligationRepository obligationRepository;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public CreateObligation(
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        ObligationRepository obligationRepository,
        TransactionManager transactionManager,
        Clock clock
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "Category repository cannot be null");
        this.obligationRepository = Objects.requireNonNull(obligationRepository,
            "Obligation repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    public UUID execute(CreateObligationCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            LocalDate today = LocalDate.now(clock);

            Account account = accountRepository.findByIdAndUserId(command.accountId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Account not found for user: " + command.accountId()));

            Category category = categoryRepository.findByIdAndUserId(command.categoryId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Category not found for user: " + command.categoryId()));

            if (obligationRepository.existsByUserIdAndName(command.userId(), command.name())) {
                throw new BusinessRuleViolationException(BusinessRuleCode.OBLIGATION_NAME_ALREADY_EXISTS,
                    ApplicationConstants.OBLIGATION_NAME_ALREADY_EXISTS_MESSAGE);
            }

            Obligation obligation = Obligation.create(
                account,
                category,
                command.name(),
                command.amount(),
                command.recurrence(),
                today
            );

            obligationRepository.create(obligation);
            return obligation.id();
        });
    }
}
