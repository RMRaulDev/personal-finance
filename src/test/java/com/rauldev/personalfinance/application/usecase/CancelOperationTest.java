package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
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
import com.rauldev.personalfinance.application.port.out.ReversalRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.Income;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;
import com.rauldev.personalfinance.domain.Reversal;

class CancelOperationTest {
    private static final LocalDate OPERATION_DATE = LocalDate.of(2026, 8, 20);

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
            transactionManager
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
            transactionManager
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
            transactionManager
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
            transactionManager
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
    void execute_shouldThrowIllegalStateExceptionWhenIncomeIsAlreadyCancelled() {
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
            transactionManager
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);

        assertThrows(IllegalStateException.class, () -> cancelOperation.execute(command));
        assertTrue(accountRepository.findCalls.isEmpty());
        assertEquals(0, reversalRepository.createCalls);
        assertEquals(0, incomeRepository.updateCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(Money.ofCents(10000), account.balance());
    }

    @Test
    void execute_shouldThrowIllegalStateExceptionWhenExpenseIsAlreadyCancelled() {
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
            transactionManager
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);

        assertThrows(IllegalStateException.class, () -> cancelOperation.execute(command));
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
            transactionManager
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
            transactionManager
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
            transactionManager
        );

        CancelOperationCommand command = new CancelOperationCommand(userId, operationId);

        assertThrows(IllegalStateException.class, () -> cancelOperation.execute(command));
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
            transactionManager
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
            transactionManager
        );

        cancelOperation.execute(new CancelOperationCommand(userId, operationId));

        assertTrue(transactionManager.executed);
    }

    @Test
    void constructor_shouldRejectNullDependencies() {
        assertThrows(NullPointerException.class,
            () -> new CancelOperation(null, null, null, null, null));
    }

    @Test
    void command_shouldRejectNullReferences() {
        assertThrows(NullPointerException.class,
            () -> new CancelOperationCommand(null, UUID.randomUUID()));
        assertThrows(NullPointerException.class,
            () -> new CancelOperationCommand(UUID.randomUUID(), null));
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
}
