package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

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
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;

class CreateObligationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final UUID userId = UUID.randomUUID();
    private final Account account = new Account(UUID.randomUUID(), userId, "Checking");
    private final Category category = new Category(UUID.randomUUID(), userId, "Rent", CategoryType.EXPENSE);
    private final RecordingAccountRepository accountRepository = new RecordingAccountRepository(account);
    private final RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(category);
    private final RecordingObligationRepository obligationRepository = new RecordingObligationRepository();
    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();

    private CreateObligation useCase() {
        return new CreateObligation(accountRepository, categoryRepository, obligationRepository,
            transactionManager, CLOCK);
    }

    private CreateObligationCommand command(UUID accountId, UUID categoryId, String name, Recurrence recurrence) {
        return new CreateObligationCommand(userId, accountId, categoryId, name, Money.ofCents(150000), recurrence);
    }

    private static Recurrence monthlyFrom(LocalDate start) {
        return new Recurrence(Frequency.MONTHLY, start, null);
    }

    @Test
    void constructor_shouldThrowWhenAccountRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new CreateObligation(
            null, categoryRepository, obligationRepository, transactionManager, CLOCK));
        assertEquals("Account repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenCategoryRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new CreateObligation(
            accountRepository, null, obligationRepository, transactionManager, CLOCK));
        assertEquals("Category repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenObligationRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new CreateObligation(
            accountRepository, categoryRepository, null, transactionManager, CLOCK));
        assertEquals("Obligation repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenTransactionManagerIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new CreateObligation(
            accountRepository, categoryRepository, obligationRepository, null, CLOCK));
        assertEquals("Transaction manager cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenClockIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new CreateObligation(
            accountRepository, categoryRepository, obligationRepository, transactionManager, null));
        assertEquals("Clock cannot be null", e.getMessage());
    }

    @Test
    void execute_shouldThrowWhenCommandIsNull() {
        assertThrows(NullPointerException.class, () -> useCase().execute(null));
    }

    @Test
    void execute_shouldPersistActiveObligationAndReturnItsId() {
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, TODAY.plusDays(10), TODAY.plusDays(400));

        UUID result = useCase().execute(command(account.id(), category.id(), "Rent", recurrence));

        Obligation created = obligationRepository.createdObligation;
        assertTrue(transactionManager.executed);
        assertEquals(1, obligationRepository.createCalls);
        assertEquals(created.id(), result);
        assertEquals(userId, created.userId());
        assertEquals("Rent", created.name());
        assertEquals(Money.ofCents(150000), created.amount());
        assertEquals(account.id(), created.accountId());
        assertEquals(category.id(), created.categoryId());
        assertEquals(recurrence, created.recurrence());
        assertEquals(ObligationStatus.ACTIVE, created.status());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenAccountDoesNotExist() {
        assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(command(UUID.randomUUID(), category.id(), "Rent", monthlyFrom(TODAY))));

        assertEquals(0, categoryRepository.findCalls);
        assertEquals(0, obligationRepository.existsCalls);
        assertEquals(0, obligationRepository.createCalls);
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenAccountBelongsToAnotherUser() {
        UUID otherUser = UUID.randomUUID();
        Account foreign = new Account(UUID.randomUUID(), otherUser, "Foreign");
        accountRepository.accounts.add(foreign);

        assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(command(foreign.id(), category.id(), "Rent", monthlyFrom(TODAY))));

        assertEquals(0, categoryRepository.findCalls);
        assertEquals(0, obligationRepository.existsCalls);
        assertEquals(0, obligationRepository.createCalls);
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenCategoryDoesNotExist() {
        assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(command(account.id(), UUID.randomUUID(), "Rent", monthlyFrom(TODAY))));

        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, categoryRepository.findCalls);
        assertEquals(0, obligationRepository.existsCalls);
        assertEquals(0, obligationRepository.createCalls);
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenCategoryBelongsToAnotherUser() {
        Category foreign = new Category(UUID.randomUUID(), UUID.randomUUID(), "Foreign", CategoryType.EXPENSE);
        categoryRepository.categories.add(foreign);

        assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(command(account.id(), foreign.id(), "Rent", monthlyFrom(TODAY))));

        assertEquals(0, obligationRepository.existsCalls);
        assertEquals(0, obligationRepository.createCalls);
    }

    @Test
    void execute_shouldThrowBusinessRuleViolationExceptionWhenNameAlreadyExists() {
        obligationRepository.seed(existingObligation("Rent"));

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> useCase().execute(command(account.id(), category.id(), "Rent", monthlyFrom(TODAY))));

        assertEquals(BusinessRuleCode.OBLIGATION_NAME_ALREADY_EXISTS, e.code());
        assertEquals(ApplicationConstants.OBLIGATION_NAME_ALREADY_EXISTS_MESSAGE, e.getMessage());
        assertEquals(1, obligationRepository.existsCalls);
        assertEquals(0, obligationRepository.createCalls);
    }

    @Test
    void execute_shouldNotRejectNameUsedByAnotherUsersObligation() {
        Obligation foreign = new Obligation(UUID.randomUUID(), UUID.randomUUID(), "Rent", Money.ofCents(100),
            UUID.randomUUID(), UUID.randomUUID(), monthlyFrom(TODAY.plusDays(5)), ObligationStatus.ACTIVE);
        obligationRepository.seed(foreign);

        UUID result = useCase().execute(command(account.id(), category.id(), "Rent", monthlyFrom(TODAY)));

        assertEquals(obligationRepository.createdObligation.id(), result);
        assertEquals(1, obligationRepository.existsCalls);
        assertEquals(1, obligationRepository.createCalls);
    }

    @Test
    void execute_shouldThrowDuplicateNameBeforeDomainRulesWhenNameExistsAndAccountIsInactive() {
        obligationRepository.seed(existingObligation("Rent"));
        account.deactivate();

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> useCase().execute(command(account.id(), category.id(), "Rent", monthlyFrom(TODAY))));

        assertEquals(BusinessRuleCode.OBLIGATION_NAME_ALREADY_EXISTS, e.code());
        assertEquals(ApplicationConstants.OBLIGATION_NAME_ALREADY_EXISTS_MESSAGE, e.getMessage());
        assertEquals(0, obligationRepository.createCalls);
    }

    @Test
    void execute_shouldCheckResourcesBeforeDuplicateName() {
        obligationRepository.seed(existingObligation("Rent"));

        assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(command(UUID.randomUUID(), category.id(), "Rent", monthlyFrom(TODAY))));

        assertEquals(0, obligationRepository.existsCalls);
    }

    @Test
    void execute_shouldAcceptStartDate31DaysBeforeToday() {
        UUID result = useCase().execute(command(account.id(), category.id(), "Rent", monthlyFrom(TODAY.minusDays(31))));

        assertEquals(obligationRepository.createdObligation.id(), result);
        assertEquals(1, obligationRepository.createCalls);
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenStartDateIs32DaysBeforeToday() {
        assertThrows(IllegalArgumentException.class,
            () -> useCase().execute(command(account.id(), category.id(), "Rent", monthlyFrom(TODAY.minusDays(32)))));

        assertEquals(0, obligationRepository.createCalls);
    }

    @Test
    void execute_shouldThrowAccountInactiveWhenAccountIsInactive() {
        account.deactivate();

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> useCase().execute(command(account.id(), category.id(), "Rent", monthlyFrom(TODAY))));

        assertEquals(BusinessRuleCode.ACCOUNT_INACTIVE, e.code());
        assertEquals("Account must be active", e.getMessage());
        assertEquals(0, obligationRepository.createCalls);
    }

    @Test
    void execute_shouldThrowCategoryInactiveWhenCategoryIsInactive() {
        category.deactivate();

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> useCase().execute(command(account.id(), category.id(), "Rent", monthlyFrom(TODAY))));

        assertEquals(BusinessRuleCode.CATEGORY_INACTIVE, e.code());
        assertEquals("Category must be active", e.getMessage());
        assertEquals(0, obligationRepository.createCalls);
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenCategoryIsIncome() {
        Category income = new Category(UUID.randomUUID(), userId, "Salary", CategoryType.INCOME);
        categoryRepository.categories.add(income);

        assertThrows(IllegalArgumentException.class,
            () -> useCase().execute(command(account.id(), income.id(), "Rent", monthlyFrom(TODAY))));

        assertEquals(0, obligationRepository.createCalls);
    }

    private Obligation existingObligation(String name) {
        return new Obligation(UUID.randomUUID(), userId, name, Money.ofCents(100), account.id(), category.id(),
            monthlyFrom(TODAY.plusDays(5)), ObligationStatus.ACTIVE);
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
        private final List<Account> accounts = new ArrayList<>();
        private int findCalls;

        private RecordingAccountRepository(Account account) {
            accounts.add(account);
        }

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
            return accounts.stream().filter(a -> a.id().equals(id) && a.userId().equals(userId)).findFirst();
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
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingCategoryRepository implements CategoryRepository {
        private final List<Category> categories = new ArrayList<>();
        private int findCalls;

        private RecordingCategoryRepository(Category category) {
            categories.add(category);
        }

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
            return categories.stream().filter(c -> c.id().equals(id) && c.userId().equals(userId)).findFirst();
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

    private static final class RecordingObligationRepository implements ObligationRepository {
        private final List<Obligation> stored = new ArrayList<>();
        private int existsCalls;
        private int createCalls;
        private Obligation createdObligation;

        private void seed(Obligation obligation) {
            stored.add(obligation);
        }

        @Override
        public Obligation create(Obligation obligation) {
            createCalls++;
            createdObligation = obligation;
            stored.add(obligation);
            return obligation;
        }

        @Override
        public Optional<Obligation> findByIdAndUserId(UUID id, UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByUserIdAndName(UUID userId, String name) {
            existsCalls++;
            return stored.stream().anyMatch(o -> o.userId().equals(userId) && o.name().equals(name));
        }

        @Override
        public boolean existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID obligationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Obligation update(Obligation obligation) {
            throw new UnsupportedOperationException();
        }
    }
}
