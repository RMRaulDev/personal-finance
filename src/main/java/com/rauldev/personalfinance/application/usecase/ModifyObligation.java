package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.OccurrenceResolution;

/**
 * Applies the requested changes in a fixed order (name, amount, payment source, recurrence) on the loaded
 * obligation and persists it once at the end, inside a single transaction: if any change is rejected, nothing is
 * persisted.
 */
public final class ModifyObligation {
    private final ObligationRepository obligationRepository;
    private final OccurrenceResolutionRepository occurrenceResolutionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public ModifyObligation(
        ObligationRepository obligationRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        TransactionManager transactionManager,
        Clock clock
    ) {
        this.obligationRepository = Objects.requireNonNull(obligationRepository,
            "Obligation repository cannot be null");
        this.occurrenceResolutionRepository = Objects.requireNonNull(occurrenceResolutionRepository,
            "Occurrence resolution repository cannot be null");
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "Category repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    public UUID execute(ModifyObligationCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            LocalDate today = LocalDate.now(clock);

            Obligation obligation = obligationRepository.findByIdAndUserId(command.obligationId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Obligation not found for user: " + command.obligationId()));

            Optional<PaymentSource> paymentSource = loadRequestedPaymentSource(command, obligation);

            List<OccurrenceResolution> resolutions = occurrenceResolutionRepository.findByObligationId(
                obligation.id());

            if (command.name() != null) {
                obligation.rename(command.name());
                if (obligationRepository.existsByUserIdAndNameAndIdNot(
                    command.userId(), command.name(), obligation.id())) {
                    throw new BusinessRuleViolationException(BusinessRuleCode.OBLIGATION_NAME_ALREADY_EXISTS,
                        ApplicationConstants.OBLIGATION_NAME_ALREADY_EXISTS_MESSAGE);
                }
            }
            if (command.amount() != null) {
                obligation.changeAmount(command.amount(), today, resolutions);
            }
            paymentSource.ifPresent(source ->
                obligation.changePaymentSource(source.account(), source.category(), today, resolutions));
            if (command.recurrence() != null) {
                obligation.changeRecurrence(command.recurrence(), today, resolutions);
            }

            obligationRepository.update(obligation);
            return obligation.id();
        });
    }

    /**
     * Loads the payment source only when the command changes it; the field not supplied is taken from the
     * obligation's current value.
     */
    private Optional<PaymentSource> loadRequestedPaymentSource(ModifyObligationCommand command, Obligation obligation) {
        if (command.accountId() == null && command.categoryId() == null) {
            return Optional.empty();
        }
        UUID accountId = command.accountId() != null ? command.accountId() : obligation.accountId();
        UUID categoryId = command.categoryId() != null ? command.categoryId() : obligation.categoryId();

        Account account = accountRepository.findByIdAndUserId(accountId, command.userId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Account not found for user: " + accountId));

        Category category = categoryRepository.findByIdAndUserId(categoryId, command.userId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Category not found for user: " + categoryId));

        return Optional.of(new PaymentSource(account, category));
    }

    private record PaymentSource(Account account, Category category) {
    }
}
