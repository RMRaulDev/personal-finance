package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.Money;

class ExpenseRegistrationTest {
    private static final LocalDate OPERATION_DATE = LocalDate.of(2026, 8, 20);

    private final UUID userId = UUID.randomUUID();
    private final Account account = new Account(UUID.randomUUID(), userId, "Checking");
    private final Category category = new Category(UUID.randomUUID(), userId, "Groceries", CategoryType.EXPENSE);
    private final RecordingAccountRepository accountRepository = new RecordingAccountRepository();
    private final RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository();
    private final RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository(accountRepository);

    private ExpenseRegistration registration() {
        accountRepository.accounts.put(account.id(), account);
        categoryRepository.categories.put(category.id(), category);
        return new ExpenseRegistration(accountRepository, categoryRepository, expenseRepository);
    }

    private RegisterExpenseCommand command(Money amount) {
        return new RegisterExpenseCommand(userId, account.id(), category.id(), amount, OPERATION_DATE);
    }

    private void assertNothingPersisted() {
        assertEquals(0, expenseRepository.createCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    void register_shouldReturnExpenseAndDebitAccount() {
        account.credit(Money.ofCents(10000));

        Expense expense = registration().register(command(Money.ofCents(4000)));

        assertEquals(userId, expense.userId());
        assertEquals(account.id(), expense.accountId());
        assertEquals(category.id(), expense.categoryId());
        assertEquals(Money.ofCents(4000), expense.amount());
        assertEquals(OPERATION_DATE, expense.operationDate());
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertEquals(1, expenseRepository.createCalls);
        assertEquals(1, accountRepository.updateCalls);
        assertSame(expense, expenseRepository.created);
        assertSame(account, accountRepository.updated);
        assertEquals(Money.ofCents(6000), accountRepository.updated.balance());
    }

    @Test
    void register_shouldCreateExpenseBeforeUpdatingAccount() {
        account.credit(Money.ofCents(10000));

        registration().register(command(Money.ofCents(4000)));

        assertEquals(Money.ofCents(6000), expenseRepository.accountBalanceAtCreate);
        assertEquals(0, expenseRepository.accountUpdatesAtCreate);
    }

    @Test
    void register_shouldAllowAmountEqualToBalance() {
        account.credit(Money.ofCents(4000));

        registration().register(command(Money.ofCents(4000)));

        assertEquals(Money.ofCents(0), accountRepository.updated.balance());
    }

    @Test
    void register_shouldRejectAmountOneCentOverBalance() {
        account.credit(Money.ofCents(3999));
        ExpenseRegistration registration = registration();

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> registration.register(command(Money.ofCents(4000))));

        assertEquals(BusinessRuleCode.INSUFFICIENT_BALANCE, e.code());
        assertEquals("Account balance is insufficient", e.getMessage());
        assertNothingPersisted();
        assertEquals(Money.ofCents(3999), account.balance());
    }

    @Test
    void register_shouldThrowResourceNotFoundExceptionAndSkipCategoryLookupWhenAccountDoesNotExist() {
        account.credit(Money.ofCents(10000));
        ExpenseRegistration registration = registration();
        accountRepository.accounts.clear();

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
            () -> registration.register(command(Money.ofCents(1000))));

        assertEquals("Account not found for user: " + account.id(), e.getMessage());
        assertEquals(1, accountRepository.findCalls);
        assertEquals(0, categoryRepository.findCalls);
        assertNothingPersisted();
    }

    @Test
    void register_shouldThrowResourceNotFoundExceptionWhenAccountBelongsToAnotherUser() {
        account.credit(Money.ofCents(10000));
        ExpenseRegistration registration = registration();

        assertThrows(ResourceNotFoundException.class, () -> registration.register(new RegisterExpenseCommand(
            UUID.randomUUID(), account.id(), category.id(), Money.ofCents(1000), OPERATION_DATE)));

        assertEquals(0, categoryRepository.findCalls);
        assertNothingPersisted();
    }

    @Test
    void register_shouldThrowResourceNotFoundExceptionWhenCategoryDoesNotExist() {
        account.credit(Money.ofCents(10000));
        ExpenseRegistration registration = registration();
        categoryRepository.categories.clear();

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
            () -> registration.register(command(Money.ofCents(1000))));

        assertEquals("Category not found for user: " + category.id(), e.getMessage());
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertNothingPersisted();
        assertEquals(Money.ofCents(10000), account.balance());
    }

    @Test
    void register_shouldRejectInactiveAccount() {
        account.credit(Money.ofCents(10000));
        account.deactivate();
        ExpenseRegistration registration = registration();

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> registration.register(command(Money.ofCents(1000))));

        assertEquals(BusinessRuleCode.ACCOUNT_INACTIVE, e.code());
        assertEquals("Account must be active", e.getMessage());
        assertNothingPersisted();
    }

    @Test
    void register_shouldRejectInactiveCategory() {
        account.credit(Money.ofCents(10000));
        category.deactivate();
        ExpenseRegistration registration = registration();

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> registration.register(command(Money.ofCents(1000))));

        assertEquals(BusinessRuleCode.CATEGORY_INACTIVE, e.code());
        assertEquals("Category must be active", e.getMessage());
        assertNothingPersisted();
    }

    @Test
    void register_shouldRejectIncomeCategory() {
        account.credit(Money.ofCents(10000));
        Category income = new Category(UUID.randomUUID(), userId, "Salary", CategoryType.INCOME);
        ExpenseRegistration registration = registration();
        categoryRepository.categories.put(income.id(), income);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> registration.register(new RegisterExpenseCommand(userId, account.id(), income.id(),
                Money.ofCents(1000), OPERATION_DATE)));

        assertEquals("Category type is not valid for an expense", e.getMessage());
        assertNothingPersisted();
    }

    @Test
    void register_shouldThrowWhenCommandIsNull() {
        ExpenseRegistration registration = registration();

        assertThrows(NullPointerException.class, () -> registration.register(null));
    }

    @Test
    void constructor_shouldThrowWhenAccountRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ExpenseRegistration(null, categoryRepository, expenseRepository));
        assertEquals("Account repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenCategoryRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ExpenseRegistration(accountRepository, null, expenseRepository));
        assertEquals("Category repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenExpenseOperationRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class,
            () -> new ExpenseRegistration(accountRepository, categoryRepository, null));
        assertEquals("Expense operation repository cannot be null", e.getMessage());
    }

    private static final class RecordingAccountRepository implements AccountRepository {
        private final Map<UUID, Account> accounts = new HashMap<>();
        private int findCalls;
        private int updateCalls;
        private Account updated;

        @Override
        public Account create(Account account) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Account> findById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Account> findByIdAndUserId(UUID id, UUID userId) {
            findCalls++;
            return Optional.ofNullable(accounts.get(id)).filter(a -> a.userId().equals(userId));
        }

        @Override
        public List<Account> findByUserId(UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByUserIdAndName(UUID userId, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Account update(Account account) {
            updateCalls++;
            updated = account;
            return account;
        }
    }

    private static final class RecordingCategoryRepository implements CategoryRepository {
        private final Map<UUID, Category> categories = new HashMap<>();
        private int findCalls;

        @Override
        public Category create(Category category) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Category> findById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Category> findByIdAndUserId(UUID id, UUID userId) {
            findCalls++;
            return Optional.ofNullable(categories.get(id)).filter(c -> c.userId().equals(userId));
        }

        @Override
        public List<Category> findByUserId(UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByUserIdAndName(UUID userId, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Category update(Category category) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingExpenseRepository implements ExpenseOperationRepository {
        private final RecordingAccountRepository accountRepository;
        private final List<Expense> all = new ArrayList<>();
        private int createCalls;
        private Expense created;
        private Money accountBalanceAtCreate;
        private int accountUpdatesAtCreate;

        private RecordingExpenseRepository(RecordingAccountRepository accountRepository) {
            this.accountRepository = accountRepository;
        }

        @Override
        public Expense create(Expense expense) {
            createCalls++;
            created = expense;
            all.add(expense);
            accountUpdatesAtCreate = accountRepository.updateCalls;
            accountBalanceAtCreate = accountRepository.accounts.get(expense.accountId()).balance();
            return expense;
        }

        @Override
        public Optional<Expense> findById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Expense> findByIdAndUserId(UUID id, UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Expense> findByUserId(UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Expense update(Expense expense) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(UUID id) {
            throw new UnsupportedOperationException();
        }
    }
}
