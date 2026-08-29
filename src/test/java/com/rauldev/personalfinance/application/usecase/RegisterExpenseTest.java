package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.List;
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
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.domain.Money;

class RegisterExpenseTest {
    private static final LocalDate OPERATION_DATE = LocalDate.of(2026, 8, 20);

    @Test
    void execute_shouldRegisterExpenseAndUpdateAccount() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Money amount = Money.ofCents(5000);

        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(10000));
        Category category = new Category(categoryId, userId, "Groceries", CategoryType.EXPENSE);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account));
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.of(category));
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterExpense registerExpense = new RegisterExpense(
            accountRepository,
            categoryRepository,
            expenseRepository,
            transactionManager
        );

        RegisterExpenseCommand command = new RegisterExpenseCommand(
            userId,
            accountId,
            categoryId,
            amount,
            OPERATION_DATE
        );

        UUID result = registerExpense.execute(command);

        assertNotNull(result);
        assertTrue(transactionManager.executed);
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertEquals(1, expenseRepository.createCalls);
        assertEquals(1, accountRepository.updateCalls);
        assertNotNull(expenseRepository.createdExpense);
        assertEquals(accountId, expenseRepository.createdExpense.accountId());
        assertEquals(categoryId, expenseRepository.createdExpense.categoryId());
        assertEquals(amount, expenseRepository.createdExpense.amount());
        assertEquals(Money.ofCents(5000), accountRepository.updatedAccount.balance());
        assertEquals(result, expenseRepository.createdExpense.id());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenAccountDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.empty());
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.of(
            new Category(categoryId, userId, "Groceries", CategoryType.EXPENSE)
        ));
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterExpense registerExpense = new RegisterExpense(
            accountRepository,
            categoryRepository,
            expenseRepository,
            transactionManager
        );

        RegisterExpenseCommand command = new RegisterExpenseCommand(
            userId,
            accountId,
            categoryId,
            Money.ofCents(2000),
            OPERATION_DATE
        );

        assertThrows(ResourceNotFoundException.class, () -> registerExpense.execute(command));
        assertEquals(1, accountRepository.findCalls);
        assertEquals(0, categoryRepository.findCalls);
        assertEquals(0, expenseRepository.createCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenCategoryDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(10000));

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account));
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.empty());
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterExpense registerExpense = new RegisterExpense(
            accountRepository,
            categoryRepository,
            expenseRepository,
            transactionManager
        );

        RegisterExpenseCommand command = new RegisterExpenseCommand(
            userId,
            accountId,
            categoryId,
            Money.ofCents(2000),
            OPERATION_DATE
        );

        assertThrows(ResourceNotFoundException.class, () -> registerExpense.execute(command));
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertEquals(0, expenseRepository.createCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenCategoryTypeIsInvalid() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Money amount = Money.ofCents(2000);

        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(10000));
        Category wrongCategory = new Category(categoryId, userId, "Salary", CategoryType.INCOME);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account));
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.of(wrongCategory));
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterExpense registerExpense = new RegisterExpense(
            accountRepository,
            categoryRepository,
            expenseRepository,
            transactionManager
        );

        RegisterExpenseCommand command = new RegisterExpenseCommand(
            userId,
            accountId,
            categoryId,
            amount,
            OPERATION_DATE
        );

        assertThrows(IllegalArgumentException.class, () -> registerExpense.execute(command));
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertEquals(0, expenseRepository.createCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenBalanceIsInsufficient() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Money amount = Money.ofCents(3000);

        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(2000));
        Category category = new Category(categoryId, userId, "Groceries", CategoryType.EXPENSE);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account));
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.of(category));
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterExpense registerExpense = new RegisterExpense(
            accountRepository,
            categoryRepository,
            expenseRepository,
            transactionManager
        );

        RegisterExpenseCommand command = new RegisterExpenseCommand(
            userId,
            accountId,
            categoryId,
            amount,
            OPERATION_DATE
        );

        assertThrows(IllegalStateException.class, () -> registerExpense.execute(command));
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertEquals(0, expenseRepository.createCalls);
        assertEquals(0, accountRepository.updateCalls);
        assertEquals(Money.ofCents(2000), account.balance());
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenAmountIsInvalid() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(5000));
        Category category = new Category(categoryId, userId, "Groceries", CategoryType.EXPENSE);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account));
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.of(category));
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterExpense registerExpense = new RegisterExpense(
            accountRepository,
            categoryRepository,
            expenseRepository,
            transactionManager
        );

        RegisterExpenseCommand command = new RegisterExpenseCommand(
            userId,
            accountId,
            categoryId,
            Money.ofCents(0),
            OPERATION_DATE
        );

        assertThrows(IllegalArgumentException.class, () -> registerExpense.execute(command));
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertEquals(0, expenseRepository.createCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    void execute_shouldUseTransactionManager() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(7000));
        Category category = new Category(categoryId, userId, "Groceries", CategoryType.EXPENSE);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account));
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.of(category));
        RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterExpense registerExpense = new RegisterExpense(
            accountRepository,
            categoryRepository,
            expenseRepository,
            transactionManager
        );

        registerExpense.execute(new RegisterExpenseCommand(
            userId,
            accountId,
            categoryId,
            Money.ofCents(100),
            OPERATION_DATE
        ));

        assertTrue(transactionManager.executed);
    }

    @Test
    void constructor_shouldRejectNullDependencies() {
        assertThrows(NullPointerException.class,
            () -> new RegisterExpense(null, null, null, null));
    }

    @Test
    void command_shouldRejectNullReferences() {
        assertThrows(NullPointerException.class,
            () -> new RegisterExpenseCommand(null, UUID.randomUUID(), UUID.randomUUID(), Money.ofCents(100), OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterExpenseCommand(UUID.randomUUID(), null, UUID.randomUUID(), Money.ofCents(100), OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterExpenseCommand(UUID.randomUUID(), UUID.randomUUID(), null, Money.ofCents(100), OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterExpenseCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterExpenseCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Money.ofCents(100), null));
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
        private final Optional<Account> account;
        private int findCalls;
        private int updateCalls;
        private Account updatedAccount;

        private RecordingAccountRepository(Optional<Account> account) {
            this.account = account;
        }

        @Override
        public Account create(Account account) {
            return account;
        }

        @Override
        public Optional<Account> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Account> findByIdAndUserId(UUID id, UUID userId) {
            findCalls++;
            return account;
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
            updateCalls++;
            updatedAccount = account;
            return account;
        }
    }

    private static final class RecordingCategoryRepository implements CategoryRepository {
        private final Optional<Category> category;
        private int findCalls;

        private RecordingCategoryRepository(Optional<Category> category) {
            this.category = category;
        }

        @Override
        public Category create(Category category) {
            return category;
        }

        @Override
        public Optional<Category> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Category> findByIdAndUserId(UUID id, UUID userId) {
            findCalls++;
            return category;
        }

        @Override
        public List<Category> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public boolean existsByUserIdAndName(UUID userId, String name) {
            return false;
        }

        @Override
        public Category update(Category category) {
            return category;
        }
    }

    private static final class RecordingExpenseRepository implements ExpenseOperationRepository {
        private int createCalls;
        private com.rauldev.personalfinance.domain.Expense createdExpense;

        @Override
        public com.rauldev.personalfinance.domain.Expense create(com.rauldev.personalfinance.domain.Expense expense) {
            createCalls++;
            createdExpense = expense;
            return expense;
        }

        @Override
        public Optional<com.rauldev.personalfinance.domain.Expense> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<com.rauldev.personalfinance.domain.Expense> findByIdAndUserId(UUID id, UUID userId) {
            return Optional.empty();
        }

        @Override
        public List<com.rauldev.personalfinance.domain.Expense> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public com.rauldev.personalfinance.domain.Expense update(com.rauldev.personalfinance.domain.Expense expense) {
            return expense;
        }

        @Override
        public void deleteById(UUID id) {
            // no-op for tests
        }
    }
}
