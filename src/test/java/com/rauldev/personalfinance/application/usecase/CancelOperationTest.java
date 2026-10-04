package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.application.port.out.IncomeOperationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.ReversalRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.Income;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OccurrenceResolution;
import com.rauldev.personalfinance.domain.OperationStatus;
import com.rauldev.personalfinance.domain.ResolutionStatus;
import com.rauldev.personalfinance.domain.Reversal;

class CancelOperationTest {
    private static final LocalDate OPERATION_DATE = LocalDate.of(2026, 8, 20);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-20T10:15:30Z"), ZoneOffset.UTC);

    @Test
    void execute_shouldCancelIncomeAndDebitAccountAndPersistReversal() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        Money amount = Money.ofCents(5000);

        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(10000));
        Income income = new Income(operationId, userId, amount, OPERATION_DATE, accountId, categoryId);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(account);
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository(income);
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CancelOperation cancelOperation = new CancelOperation(
            accountRepository,
            incomeRepository,
            expenseRepository,
            reversalRepository,
            new RecordingResolutionRepository(),
            transactionManager,
            CLOCK
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);
        UUID result = cancelOperation.execute(command);

        assertNotNull(result);
        assertTrue(transactionManager.executed);
        assertEquals(1, incomeRepository.findCalls.size());
        assertEquals(operationId, incomeRepository.findCalls.get(0));
        assertEquals(0, expenseRepository.findCalls.size());
        assertEquals(1, accountRepository.findCalls.size());
        assertEquals(accountId, accountRepository.findCalls.get(0));
        assertEquals(1, reversalRepository.createCalls);
        assertEquals(1, incomeRepository.updateCalls);
        assertEquals(1, accountRepository.updatedAccounts.size());
        assertEquals(OperationStatus.CANCELLED, income.status());
        assertEquals(Money.ofCents(5000), account.balance());
        assertNotNull(reversalRepository.createdReversal);
        assertEquals(operationId, reversalRepository.createdReversal.originalOperationId());
        assertEquals(userId, reversalRepository.createdReversal.userId());
        assertEquals(amount, reversalRepository.createdReversal.amount());
        assertEquals(result, reversalRepository.createdReversal.id());
    }

    @Test
    void execute_shouldCancelExpenseAndCreditAccountAndPersistReversal() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        Money amount = Money.ofCents(3000);

        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(2000));
        Expense expense = new Expense(operationId, userId, amount, OPERATION_DATE, accountId, categoryId);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(account);
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository(expense);
        RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CancelOperation cancelOperation = new CancelOperation(
            accountRepository,
            incomeRepository,
            expenseRepository,
            reversalRepository,
            new RecordingResolutionRepository(),
            transactionManager,
            CLOCK
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);
        UUID result = cancelOperation.execute(command);

        assertNotNull(result);
        assertTrue(transactionManager.executed);
        assertEquals(1, incomeRepository.findCalls.size());
        assertEquals(1, expenseRepository.findCalls.size());
        assertEquals(operationId, expenseRepository.findCalls.get(0));
        assertEquals(1, accountRepository.findCalls.size());
        assertEquals(accountId, accountRepository.findCalls.get(0));
        assertEquals(1, reversalRepository.createCalls);
        assertEquals(1, expenseRepository.updateCalls);
        assertEquals(1, accountRepository.updatedAccounts.size());
        assertEquals(OperationStatus.CANCELLED, expense.status());
        assertEquals(Money.ofCents(5000), account.balance());
        assertNotNull(reversalRepository.createdReversal);
        assertEquals(operationId, reversalRepository.createdReversal.originalOperationId());
        assertEquals(userId, reversalRepository.createdReversal.userId());
        assertEquals(amount, reversalRepository.createdReversal.amount());
        assertEquals(result, reversalRepository.createdReversal.id());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenNeitherIncomeNorExpenseExists() {
        UUID userId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        RecordingAccountRepository accountRepository = new RecordingAccountRepository();
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CancelOperation cancelOperation = new CancelOperation(
            accountRepository,
            incomeRepository,
            expenseRepository,
            reversalRepository,
            new RecordingResolutionRepository(),
            transactionManager,
            CLOCK
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);

        assertThrows(ResourceNotFoundException.class, () -> cancelOperation.execute(command));
        assertEquals(1, incomeRepository.findCalls.size());
        assertEquals(1, expenseRepository.findCalls.size());
        assertTrue(accountRepository.findCalls.isEmpty());
        assertEquals(0, reversalRepository.createCalls);
        assertEquals(0, incomeRepository.updateCalls);
        assertEquals(0, expenseRepository.updateCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
    }

    @Test
    void execute_shouldCancelExpenseAfterIncomeNotFound() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        Money amount = Money.ofCents(4000);

        Account account = new Account(accountId, userId, "Checking");
        Expense expense = new Expense(operationId, userId, amount, OPERATION_DATE, accountId, categoryId);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(account);
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository(expense);
        RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CancelOperation cancelOperation = new CancelOperation(
            accountRepository,
            incomeRepository,
            expenseRepository,
            reversalRepository,
            new RecordingResolutionRepository(),
            transactionManager,
            CLOCK
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);
        UUID result = cancelOperation.execute(command);

        assertNotNull(result);
        assertEquals(1, incomeRepository.findCalls.size());
        assertEquals(1, expenseRepository.findCalls.size());
        assertEquals(OperationStatus.CANCELLED, expense.status());
        assertEquals(1, reversalRepository.createCalls);
    }

    @Test
    void execute_shouldThrowBusinessRuleViolationExceptionWhenIncomeIsAlreadyCancelled() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(10000));
        Income income = new Income(operationId, userId, Money.ofCents(5000), OPERATION_DATE, accountId, categoryId);
        income.cancel();

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(account);
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository(income);
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CancelOperation cancelOperation = new CancelOperation(
            accountRepository,
            incomeRepository,
            expenseRepository,
            reversalRepository,
            new RecordingResolutionRepository(),
            transactionManager,
            CLOCK
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
            () -> cancelOperation.execute(command));
        assertEquals(BusinessRuleCode.OPERATION_ALREADY_CANCELLED, exception.code());
        assertEquals("Income operation is already cancelled", exception.getMessage());
        assertTrue(accountRepository.findCalls.isEmpty());
        assertEquals(0, reversalRepository.createCalls);
        assertEquals(0, incomeRepository.updateCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(Money.ofCents(10000), account.balance());
    }

    @Test
    void execute_shouldThrowBusinessRuleViolationExceptionWhenExpenseIsAlreadyCancelled() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(10000));
        Expense expense = new Expense(operationId, userId, Money.ofCents(3000), OPERATION_DATE, accountId, categoryId);
        expense.cancel();

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(account);
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository(expense);
        RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CancelOperation cancelOperation = new CancelOperation(
            accountRepository,
            incomeRepository,
            expenseRepository,
            reversalRepository,
            new RecordingResolutionRepository(),
            transactionManager,
            CLOCK
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
            () -> cancelOperation.execute(command));
        assertEquals(BusinessRuleCode.OPERATION_ALREADY_CANCELLED, exception.code());
        assertEquals("Expense operation is already cancelled", exception.getMessage());
        assertTrue(accountRepository.findCalls.isEmpty());
        assertEquals(0, reversalRepository.createCalls);
        assertEquals(0, expenseRepository.updateCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(Money.ofCents(10000), account.balance());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenAccountForIncomeDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        Income income = new Income(operationId, userId, Money.ofCents(5000), OPERATION_DATE, accountId, categoryId);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository();
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository(income);
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CancelOperation cancelOperation = new CancelOperation(
            accountRepository,
            incomeRepository,
            expenseRepository,
            reversalRepository,
            new RecordingResolutionRepository(),
            transactionManager,
            CLOCK
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);

        assertThrows(ResourceNotFoundException.class, () -> cancelOperation.execute(command));
        assertEquals(1, accountRepository.findCalls.size());
        assertEquals(0, reversalRepository.createCalls);
        assertEquals(0, incomeRepository.updateCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(OperationStatus.ACTIVE, income.status());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenAccountForExpenseDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        Expense expense = new Expense(operationId, userId, Money.ofCents(3000), OPERATION_DATE, accountId, categoryId);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository();
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository(expense);
        RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CancelOperation cancelOperation = new CancelOperation(
            accountRepository,
            incomeRepository,
            expenseRepository,
            reversalRepository,
            new RecordingResolutionRepository(),
            transactionManager,
            CLOCK
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);

        assertThrows(ResourceNotFoundException.class, () -> cancelOperation.execute(command));
        assertEquals(1, accountRepository.findCalls.size());
        assertEquals(0, reversalRepository.createCalls);
        assertEquals(0, expenseRepository.updateCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(OperationStatus.ACTIVE, expense.status());
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenIncomeReversalFailsDueToInsufficientBalance() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(2000));
        Income income = new Income(operationId, userId, Money.ofCents(5000), OPERATION_DATE, accountId, categoryId);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(account);
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository(income);
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CancelOperation cancelOperation = new CancelOperation(
            accountRepository,
            incomeRepository,
            expenseRepository,
            reversalRepository,
            new RecordingResolutionRepository(),
            transactionManager,
            CLOCK
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
            () -> cancelOperation.execute(command));
        assertEquals(BusinessRuleCode.INSUFFICIENT_BALANCE, exception.code());
        assertEquals("Account balance is insufficient", exception.getMessage());
        assertEquals(0, reversalRepository.createCalls);
        assertEquals(0, incomeRepository.updateCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(Money.ofCents(2000), account.balance());
        assertEquals(OperationStatus.ACTIVE, income.status());
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenExpenseReversalAmountIsInvalid() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        Account account = new Account(accountId, userId, "Checking");
        Expense expense = new Expense(operationId, userId, Money.ofCents(3000), OPERATION_DATE, accountId, categoryId);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(account);
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository(expense);
        RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CancelOperation cancelOperation = new CancelOperation(
            accountRepository,
            incomeRepository,
            expenseRepository,
            reversalRepository,
            new RecordingResolutionRepository(),
            transactionManager,
            CLOCK
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);
        UUID result = cancelOperation.execute(command);

        assertNotNull(result);
        assertEquals(1, reversalRepository.createCalls);
        assertEquals(1, expenseRepository.updateCalls);
        assertEquals(1, accountRepository.updatedAccounts.size());
    }

    @Test
    void execute_shouldUseTransactionManager() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(10000));
        Income income = new Income(operationId, userId, Money.ofCents(5000), OPERATION_DATE, accountId, categoryId);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(account);
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository(income);
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CancelOperation cancelOperation = new CancelOperation(
            accountRepository,
            incomeRepository,
            expenseRepository,
            reversalRepository,
            new RecordingResolutionRepository(),
            transactionManager,
            CLOCK
        );

        cancelOperation.execute(new CancelOperationCommand(userId, operationId));

        assertTrue(transactionManager.executed);
    }

    @Test
    void constructor_shouldRejectNullDependencies() {
        assertThrows(NullPointerException.class,
            () -> new CancelOperation(null, null, null, null, null, null, null));
    }

    @Test
    void execute_shouldDeletePaidResolutionAfterCancellingExpense() {
        Fixture f = new Fixture(true);
        OccurrenceResolution resolution = f.paidResolution();
        int[] seenAtLookup = new int[3];
        f.resolutionRepository.onFind = () -> {
            seenAtLookup[0] = f.reversalRepository.createCalls;
            seenAtLookup[1] = f.expenseRepository.updateCalls;
            seenAtLookup[2] = f.accountRepository.updatedAccounts.size();
        };

        f.cancel();

        assertEquals(List.of(f.operationId), f.resolutionRepository.findByExpenseIdCalls);
        assertEquals(List.of(resolution.id()), f.resolutionRepository.deleted);
        assertTrue(f.resolutionRepository.resolutions.isEmpty());
        assertEquals(1, seenAtLookup[0]);
        assertEquals(1, seenAtLookup[1]);
        assertEquals(1, seenAtLookup[2]);
        assertEquals(OperationStatus.CANCELLED, f.expense.status());
        assertEquals(Money.ofCents(13000), f.account.balance());
    }

    @Test
    void execute_shouldOnlyLookUpResolutionWhenCancellingExpenseWithoutResolution() {
        Fixture f = new Fixture(true);

        f.cancel();

        assertEquals(List.of(f.operationId), f.resolutionRepository.findByExpenseIdCalls);
        assertTrue(f.resolutionRepository.deleted.isEmpty());
        assertEquals(1, f.reversalRepository.createCalls);
    }

    @Test
    void execute_shouldNotTouchResolutionsWhenCancellingIncome() {
        Fixture f = new Fixture(false);

        f.cancel();

        assertTrue(f.resolutionRepository.findByExpenseIdCalls.isEmpty());
        assertTrue(f.resolutionRepository.deleted.isEmpty());
        assertEquals(1, f.reversalRepository.createCalls);
    }

    @Test
    void execute_shouldNotTouchResolutionsWhenExpenseIsAlreadyCancelled() {
        Fixture f = new Fixture(true);
        f.paidResolution();
        f.expense.cancel();

        assertThrows(BusinessRuleViolationException.class, f::cancel);

        assertTrue(f.resolutionRepository.findByExpenseIdCalls.isEmpty());
        assertTrue(f.resolutionRepository.deleted.isEmpty());
        assertEquals(1, f.resolutionRepository.resolutions.size());
    }

    @Test
    void execute_shouldNotTouchResolutionsWhenAccountOfExpenseDoesNotExist() {
        Fixture f = new Fixture(true);
        f.paidResolution();
        CancelOperation useCase = new CancelOperation(new RecordingAccountRepository(), f.incomeRepository,
            f.expenseRepository, f.reversalRepository, f.resolutionRepository, f.transactionManager, CLOCK);

        assertThrows(ResourceNotFoundException.class,
            () -> useCase.execute(new CancelOperationCommand(f.userId, f.operationId)));

        assertTrue(f.resolutionRepository.findByExpenseIdCalls.isEmpty());
        assertTrue(f.resolutionRepository.deleted.isEmpty());
        assertEquals(1, f.resolutionRepository.resolutions.size());
    }

    @Test
    void execute_shouldNotTouchResolutionsWhenOperationBelongsToAnotherUser() {
        Fixture f = new Fixture(true);
        f.paidResolution();
        CancelOperation useCase = f.useCase();

        assertThrows(ResourceNotFoundException.class,
            () -> useCase.execute(new CancelOperationCommand(UUID.randomUUID(), f.operationId)));

        assertTrue(f.resolutionRepository.findByExpenseIdCalls.isEmpty());
        assertTrue(f.resolutionRepository.deleted.isEmpty());
    }

    @Test
    void execute_shouldRecordReversalWithClockInstantWhenCancellingIncome() {
        Fixture f = new Fixture(false);

        f.cancel();

        assertEquals(Instant.now(CLOCK), f.reversalRepository.createdReversal.cancelledAt());
    }

    @Test
    void execute_shouldRecordReversalWithClockInstantWhenCancellingExpense() {
        Fixture f = new Fixture(true);

        f.cancel();

        assertEquals(Instant.now(CLOCK), f.reversalRepository.createdReversal.cancelledAt());
    }

    @Test
    void constructor_shouldThrowWhenOccurrenceResolutionRepositoryIsNull() {
        Fixture f = new Fixture(true);

        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new CancelOperation(f.accountRepository, f.incomeRepository, f.expenseRepository,
                f.reversalRepository, null, f.transactionManager, CLOCK));

        assertEquals("Occurrence resolution repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenClockIsNull() {
        Fixture f = new Fixture(true);

        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new CancelOperation(f.accountRepository, f.incomeRepository, f.expenseRepository,
                f.reversalRepository, f.resolutionRepository, f.transactionManager, null));

        assertEquals("Clock cannot be null", e.getMessage());
    }

    @Test
    void command_shouldRejectNullReferences() {
        assertThrows(NullPointerException.class,
            () -> new CancelOperationCommand(null, UUID.randomUUID()));
        assertThrows(NullPointerException.class,
            () -> new CancelOperationCommand(UUID.randomUUID(), null));
    }

    private static final class Fixture {
        private final UUID userId = UUID.randomUUID();
        private final UUID accountId = UUID.randomUUID();
        private final UUID categoryId = UUID.randomUUID();
        private final UUID operationId = UUID.randomUUID();
        private final Account account = new Account(accountId, userId, "Checking");
        private final RecordingAccountRepository accountRepository = new RecordingAccountRepository(account);
        private final RecordingIncomeRepository incomeRepository;
        private final RecordingExpenseRepository expenseRepository;
        private final RecordingReversalRepository reversalRepository = new RecordingReversalRepository();
        private final RecordingResolutionRepository resolutionRepository = new RecordingResolutionRepository();
        private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        private final Expense expense;
        private final Income income;

        private Fixture(boolean expenseOperation) {
            account.credit(Money.ofCents(10000));
            if (expenseOperation) {
                expense = new Expense(operationId, userId, Money.ofCents(3000), OPERATION_DATE, accountId,
                    categoryId);
                income = null;
                incomeRepository = new RecordingIncomeRepository();
                expenseRepository = new RecordingExpenseRepository(expense);
            } else {
                income = new Income(operationId, userId, Money.ofCents(3000), OPERATION_DATE, accountId, categoryId);
                expense = null;
                incomeRepository = new RecordingIncomeRepository(income);
                expenseRepository = new RecordingExpenseRepository();
            }
        }

        private CancelOperation useCase() {
            return new CancelOperation(accountRepository, incomeRepository, expenseRepository, reversalRepository,
                resolutionRepository, transactionManager, CLOCK);
        }

        private UUID cancel() {
            return useCase().execute(new CancelOperationCommand(userId, operationId));
        }

        private OccurrenceResolution paidResolution() {
            OccurrenceResolution resolution = new OccurrenceResolution(UUID.randomUUID(), UUID.randomUUID(),
                OPERATION_DATE, ResolutionStatus.PAID, operationId, Instant.parse("2026-08-01T00:00:00Z"));
            resolutionRepository.resolutions.add(resolution);
            return resolution;
        }
    }

    private static final class RecordingTransactionManager implements TransactionManager {
        private boolean executed;

        @Override
        public <T> T execute(Supplier<T> transactionalWork) {
            executed = true;
            return transactionalWork.get();
        }
    }

    private static final class RecordingAccountRepository implements AccountRepository {
        private final Map<UUID, Account> accounts = new HashMap<>();
        private final List<UUID> findCalls = new ArrayList<>();
        private final List<Account> updatedAccounts = new ArrayList<>();

        private RecordingAccountRepository(Account... initialAccounts) {
            for (Account account : initialAccounts) {
                if (account != null) {
                    accounts.put(account.id(), account);
                }
            }
        }

        @Override
        public Account create(Account account) {
            return account;
        }

        @Override
        public Optional<Account> findById(UUID id) {
            return Optional.ofNullable(accounts.get(id));
        }

        @Override
        public Optional<Account> findByIdAndUserId(UUID id, UUID userId) {
            findCalls.add(id);
            Account account = accounts.get(id);
            if (account != null && account.userId().equals(userId)) {
                return Optional.of(account);
            }
            return Optional.empty();
        }

        @Override
        public List<Account> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public boolean existsByUserIdAndName(UUID userId, String name) {
            return false;
        }

        @Override
        public Account update(Account account) {
            updatedAccounts.add(account);
            return account;
        }
    }

    private static final class RecordingIncomeRepository implements IncomeOperationRepository {
        private final Map<UUID, Income> incomes = new HashMap<>();
        private final List<UUID> findCalls = new ArrayList<>();
        private int updateCalls;

        private RecordingIncomeRepository(Income... initialIncomes) {
            for (Income income : initialIncomes) {
                if (income != null) {
                    incomes.put(income.id(), income);
                }
            }
        }

        @Override
        public Income create(Income income) {
            return income;
        }

        @Override
        public Optional<Income> findById(UUID id) {
            return Optional.ofNullable(incomes.get(id));
        }

        @Override
        public Optional<Income> findByIdAndUserId(UUID id, UUID userId) {
            findCalls.add(id);
            Income income = incomes.get(id);
            if (income != null && income.userId().equals(userId)) {
                return Optional.of(income);
            }
            return Optional.empty();
        }

        @Override
        public List<Income> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public Income update(Income income) {
            updateCalls++;
            return income;
        }

        @Override
        public void deleteById(UUID id) {
        }
    }

    private static final class RecordingExpenseRepository implements ExpenseOperationRepository {
        private final Map<UUID, Expense> expenses = new HashMap<>();
        private final List<UUID> findCalls = new ArrayList<>();
        private int updateCalls;

        private RecordingExpenseRepository(Expense... initialExpenses) {
            for (Expense expense : initialExpenses) {
                if (expense != null) {
                    expenses.put(expense.id(), expense);
                }
            }
        }

        @Override
        public Expense create(Expense expense) {
            return expense;
        }

        @Override
        public Optional<Expense> findById(UUID id) {
            return Optional.ofNullable(expenses.get(id));
        }

        @Override
        public Optional<Expense> findByIdAndUserId(UUID id, UUID userId) {
            findCalls.add(id);
            Expense expense = expenses.get(id);
            if (expense != null && expense.userId().equals(userId)) {
                return Optional.of(expense);
            }
            return Optional.empty();
        }

        @Override
        public List<Expense> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public Expense update(Expense expense) {
            updateCalls++;
            return expense;
        }

        @Override
        public void deleteById(UUID id) {
        }
    }

    private static final class RecordingReversalRepository implements ReversalRepository {
        private int createCalls;
        private Reversal createdReversal;

        @Override
        public Reversal create(Reversal reversal) {
            createCalls++;
            createdReversal = reversal;
            return reversal;
        }

        @Override
        public Optional<Reversal> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Reversal> findByOriginalOperationId(UUID operationId) {
            return Optional.empty();
        }

        @Override
        public void deleteById(UUID id) {
        }
    }

    private static final class RecordingResolutionRepository implements OccurrenceResolutionRepository {
        private final List<OccurrenceResolution> resolutions = new ArrayList<>();
        private final List<UUID> findByExpenseIdCalls = new ArrayList<>();
        private final List<UUID> deleted = new ArrayList<>();
        private Runnable onFind = () -> { };

        @Override
        public OccurrenceResolution create(OccurrenceResolution resolution) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OccurrenceResolution> findByObligationId(UUID obligationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OccurrenceResolution> findByObligationIdAndDueDate(UUID obligationId, LocalDate dueDate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OccurrenceResolution> findByExpenseId(UUID expenseId) {
            findByExpenseIdCalls.add(expenseId);
            onFind.run();
            return resolutions.stream().filter(r -> r.expenseId().filter(expenseId::equals).isPresent()).findFirst();
        }

        @Override
        public void delete(UUID resolutionId) {
            deleted.add(resolutionId);
            resolutions.removeIf(r -> r.id().equals(resolutionId));
        }
    }
}
