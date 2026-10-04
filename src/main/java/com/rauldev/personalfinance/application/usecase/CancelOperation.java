package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.application.port.out.IncomeOperationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.ReversalRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.Income;
import com.rauldev.personalfinance.domain.Reversal;

/**
 * Cancels an income or expense, reverts the balance, and records a {@link Reversal}. Cancelling an expense that paid
 * an occurrence also deletes that {@code PAID} resolution in the same transaction, so the occurrence is pending or
 * overdue again.
 */
public final class CancelOperation {
    private final AccountRepository accountRepository;
    private final IncomeOperationRepository incomeOperationRepository;
    private final ExpenseOperationRepository expenseOperationRepository;
    private final ReversalRepository reversalRepository;
    private final OccurrenceResolutionRepository occurrenceResolutionRepository;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public CancelOperation(
        AccountRepository accountRepository,
        IncomeOperationRepository incomeOperationRepository,
        ExpenseOperationRepository expenseOperationRepository,
        ReversalRepository reversalRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        TransactionManager transactionManager,
        Clock clock
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository cannot be null");
        this.incomeOperationRepository = Objects.requireNonNull(incomeOperationRepository,
            "Income operation repository cannot be null");
        this.expenseOperationRepository = Objects.requireNonNull(expenseOperationRepository,
            "Expense operation repository cannot be null");
        this.reversalRepository = Objects.requireNonNull(reversalRepository, "Reversal repository cannot be null");
        this.occurrenceResolutionRepository = Objects.requireNonNull(occurrenceResolutionRepository,
            "Occurrence resolution repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    public UUID execute(CancelOperationCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        return transactionManager.execute(() -> {
            Instant now = Instant.now(clock);

            Optional<Income> income = incomeOperationRepository.findByIdAndUserId(
                command.operationId(),
                command.userId()
            );

            if (income.isPresent()) {
                return cancelIncome(income.get(), command.userId(), now);
            }

            Optional<Expense> expense = expenseOperationRepository.findByIdAndUserId(
                command.operationId(),
                command.userId()
            );

            if (expense.isPresent()) {
                return cancelExpense(expense.get(), command.userId(), now);
            }

            throw new ResourceNotFoundException(
                "Operation not found or not cancelable: " + command.operationId()
            );
        });
    }

    private UUID cancelIncome(Income income, UUID userId, Instant now) {
        income.ensureCancellable();

        Account account = accountRepository.findByIdAndUserId(income.accountId(), userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Account not found for income cancellation: " + income.accountId()));

        Reversal reversal = new Reversal(income, now);
        account.debit(income.amount());
        income.cancel();

        reversalRepository.create(reversal);
        incomeOperationRepository.update(income);
        accountRepository.update(account);

        return reversal.id();
    }

    private UUID cancelExpense(Expense expense, UUID userId, Instant now) {
        expense.ensureCancellable();

        Account account = accountRepository.findByIdAndUserId(expense.accountId(), userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Account not found for expense cancellation: " + expense.accountId()));

        Reversal reversal = new Reversal(expense, now);
        account.credit(expense.amount());
        expense.cancel();

        reversalRepository.create(reversal);
        expenseOperationRepository.update(expense);
        accountRepository.update(account);

        // The expense was loaded by user, which checks ownership of the resolution that references it.
        occurrenceResolutionRepository.findByExpenseId(expense.id())
            .ifPresent(resolution -> occurrenceResolutionRepository.delete(resolution.id()));

        return reversal.id();
    }
}
