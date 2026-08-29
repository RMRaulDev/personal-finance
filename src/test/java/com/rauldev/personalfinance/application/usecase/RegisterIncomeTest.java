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
import com.rauldev.personalfinance.application.port.out.IncomeOperationRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.domain.Income;
import com.rauldev.personalfinance.domain.Money;

class RegisterIncomeTest {
    private static final LocalDate OPERATION_DATE = LocalDate.of(2026, 8, 20);

    @Test
    void execute_shouldRegisterIncomeAndUpdateAccount() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Money amount = Money.ofCents(5000);

        Account account = new Account(accountId, userId, "Checking");
        Category category = new Category(categoryId, userId, "Salary", CategoryType.INCOME);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account));
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.of(category));
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterIncome registerIncome = new RegisterIncome(
            accountRepository,
            categoryRepository,
            incomeRepository,
            transactionManager
        );

        RegisterIncomeCommand command = new RegisterIncomeCommand(
            userId,
            accountId,
            categoryId,
            amount,
            OPERATION_DATE
        );

        UUID result = registerIncome.execute(command);

        assertNotNull(result);
        assertTrue(transactionManager.executed);
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertEquals(1, incomeRepository.createCalls);
        assertEquals(1, accountRepository.updateCalls);
        assertNotNull(incomeRepository.createdIncome);
        assertEquals(accountId, incomeRepository.createdIncome.accountId());
        assertEquals(categoryId, incomeRepository.createdIncome.categoryId());
        assertEquals(amount, incomeRepository.createdIncome.amount());
        assertEquals(Money.ofCents(5000), accountRepository.updatedAccount.balance());
        assertEquals(result, incomeRepository.createdIncome.id());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenAccountDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.empty());
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.of(
            new Category(categoryId, userId, "Salary", CategoryType.INCOME)
        ));
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterIncome registerIncome = new RegisterIncome(
            accountRepository,
            categoryRepository,
            incomeRepository,
            transactionManager
        );

        RegisterIncomeCommand command = new RegisterIncomeCommand(
            userId,
            accountId,
            categoryId,
            Money.ofCents(2000),
            OPERATION_DATE
        );

        assertThrows(ResourceNotFoundException.class, () -> registerIncome.execute(command));
        assertEquals(1, accountRepository.findCalls);
        assertEquals(0, categoryRepository.findCalls);
        assertEquals(0, incomeRepository.createCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenCategoryDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Account account = new Account(accountId, userId, "Checking");

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account));
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.empty());
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterIncome registerIncome = new RegisterIncome(
            accountRepository,
            categoryRepository,
            incomeRepository,
            transactionManager
        );

        RegisterIncomeCommand command = new RegisterIncomeCommand(
            userId,
            accountId,
            categoryId,
            Money.ofCents(2000),
            OPERATION_DATE
        );

        assertThrows(ResourceNotFoundException.class, () -> registerIncome.execute(command));
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertEquals(0, incomeRepository.createCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenCategoryTypeIsInvalid() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Money amount = Money.ofCents(2000);

        Account account = new Account(accountId, userId, "Checking");
        Category wrongCategory = new Category(categoryId, userId, "Food", CategoryType.EXPENSE);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account));
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.of(wrongCategory));
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterIncome registerIncome = new RegisterIncome(
            accountRepository,
            categoryRepository,
            incomeRepository,
            transactionManager
        );

        RegisterIncomeCommand command = new RegisterIncomeCommand(
            userId,
            accountId,
            categoryId,
            amount,
            OPERATION_DATE
        );

        assertThrows(IllegalArgumentException.class, () -> registerIncome.execute(command));
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertEquals(0, incomeRepository.createCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenAmountIsInvalid() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Account account = new Account(accountId, userId, "Checking");
        Category category = new Category(categoryId, userId, "Salary", CategoryType.INCOME);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account));
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.of(category));
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterIncome registerIncome = new RegisterIncome(
            accountRepository,
            categoryRepository,
            incomeRepository,
            transactionManager
        );

        RegisterIncomeCommand command = new RegisterIncomeCommand(
            userId,
            accountId,
            categoryId,
            Money.ofCents(0),
            OPERATION_DATE
        );

        assertThrows(IllegalArgumentException.class, () -> registerIncome.execute(command));
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertEquals(0, incomeRepository.createCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    void execute_shouldUseTransactionManager() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Account account = new Account(accountId, userId, "Checking");
        Category category = new Category(categoryId, userId, "Salary", CategoryType.INCOME);

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account));
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(Optional.of(category));
        RecordingIncomeRepository incomeRepository = new RecordingIncomeRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterIncome registerIncome = new RegisterIncome(
            accountRepository,
            categoryRepository,
            incomeRepository,
            transactionManager
        );

        registerIncome.execute(new RegisterIncomeCommand(
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
            () -> new RegisterIncome(null, null, null, null));
    }

    @Test
    void command_shouldRejectNullReferences() {
        assertThrows(NullPointerException.class,
            () -> new RegisterIncomeCommand(null, UUID.randomUUID(), UUID.randomUUID(), Money.ofCents(100), OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterIncomeCommand(UUID.randomUUID(), null, UUID.randomUUID(), Money.ofCents(100), OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterIncomeCommand(UUID.randomUUID(), UUID.randomUUID(), null, Money.ofCents(100), OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterIncomeCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterIncomeCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Money.ofCents(100), null));
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

    private static final class RecordingIncomeRepository implements IncomeOperationRepository {
        private int createCalls;
        private Income createdIncome;

        @Override
        public Income create(Income income) {
            createCalls++;
            createdIncome = income;
            return income;
        }

        @Override
        public Optional<Income> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Income> findByIdAndUserId(UUID id, UUID userId) {
            return Optional.empty();
        }

        @Override
        public List<Income> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public Income update(Income income) {
            return income;
        }

        @Override
        public void deleteById(UUID id) {
            // no-op for tests
        }
    }
}
