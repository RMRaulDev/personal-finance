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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.OccurrenceResolution;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.domain.ResolutionStatus;

class PayOccurrenceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final Instant NOW = TODAY.atStartOfDay().plusHours(10).toInstant(ZoneOffset.UTC);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    /** Weekly (Thursdays) from 2026-08-06: 08-13 is overdue, 08-20 is today, 08-27 is future. */
    private static final LocalDate OVERDUE = LocalDate.of(2026, 8, 13);
    private static final LocalDate FUTURE = LocalDate.of(2026, 8, 27);

    private final UUID userId = UUID.randomUUID();
    private final Account account = new Account(UUID.randomUUID(), userId, "Checking");
    private final Category category = new Category(UUID.randomUUID(), userId, "Rent", CategoryType.EXPENSE);
    private final RecordingObligationRepository obligationRepository = new RecordingObligationRepository();
    private final RecordingResolutionRepository resolutionRepository = new RecordingResolutionRepository();
    private final RecordingAccountRepository accountRepository = new RecordingAccountRepository();
    private final RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository();
    private final RecordingExpenseRepository expenseRepository = new RecordingExpenseRepository();
    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();
    private Obligation obligation;

    private static LocalDate d(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    PayOccurrenceTest() {
        account.credit(Money.ofCents(50000));
        accountRepository.accounts.put(account.id(), account);
        categoryRepository.categories.put(category.id(), category);
        obligation = obligation(ObligationStatus.ACTIVE);
    }

    private Obligation obligation(ObligationStatus status) {
        Obligation created = new Obligation(UUID.randomUUID(), userId, "Rent", Money.ofCents(10000),
            account.id(), category.id(), new Recurrence(Frequency.WEEKLY, d(2026, 8, 6), null), status);
        obligationRepository.stored.add(created);
        return created;
    }

    private OccurrenceResolution existing(LocalDate dueDate, ResolutionStatus status) {
        UUID expenseId = status == ResolutionStatus.PAID ? UUID.randomUUID() : null;
        OccurrenceResolution resolution = new OccurrenceResolution(UUID.randomUUID(), obligation.id(), dueDate,
            status, expenseId, Instant.parse("2026-08-01T00:00:00Z"));
        resolutionRepository.resolutions.add(resolution);
        return resolution;
    }

    private PayOccurrence useCase() {
        return new PayOccurrence(obligationRepository, resolutionRepository,
            new ExpenseRegistration(accountRepository, categoryRepository, expenseRepository),
            transactionManager, CLOCK);
    }

    private UUID pay(LocalDate dueDate) {
        return pay(dueDate, null, null, null, null);
    }

    private UUID pay(LocalDate dueDate, Money amount, LocalDate operationDate, UUID accountId, UUID categoryId) {
        return useCase().execute(new PayOccurrenceCommand(userId, obligation.id(), dueDate, amount, operationDate,
            accountId, categoryId));
    }

    private static void assertRule(BusinessRuleCode code, String message, Runnable action) {
        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class, action::run);
        assertEquals(code, e.code());
        assertEquals(message, e.getMessage());
    }

    private void assertNothingPersisted() {
        assertEquals(0, expenseRepository.created.size());
        assertEquals(0, accountRepository.updated.size());
        assertEquals(0, resolutionRepository.created.size());
    }

    private Account otherAccount(UUID owner) {
        Account other = new Account(UUID.randomUUID(), owner, "Savings");
        other.credit(Money.ofCents(50000));
        accountRepository.accounts.put(other.id(), other);
        return other;
    }

    private Category otherCategory(UUID owner, CategoryType type) {
        Category other = new Category(UUID.randomUUID(), owner, "Other", type);
        categoryRepository.categories.put(other.id(), other);
        return other;
    }

    @Test
    void execute_shouldUseObligationDefaultsAndReturnExpenseId() {
        UUID result = pay(OVERDUE);

        assertTrue(transactionManager.executed);
        assertEquals(1, transactionManager.executions);
        assertEquals(1, expenseRepository.created.size());
        Expense expense = expenseRepository.created.get(0);
        assertEquals(expense.id(), result);
        assertEquals(Money.ofCents(10000), expense.amount());
        assertEquals(TODAY, expense.operationDate());
        assertEquals(account.id(), expense.accountId());
        assertEquals(category.id(), expense.categoryId());
        assertEquals(userId, expense.userId());
        assertEquals(Money.ofCents(40000), account.balance());
        assertEquals(1, accountRepository.updated.size());
    }

    @Test
    void execute_shouldCreatePaidResolutionLinkedToExpense() {
        UUID result = pay(OVERDUE);

        assertEquals(1, resolutionRepository.created.size());
        OccurrenceResolution saved = resolutionRepository.created.get(0);
        assertEquals(obligation.id(), saved.obligationId());
        assertEquals(OVERDUE, saved.dueDate());
        assertEquals(ResolutionStatus.PAID, saved.status());
        assertEquals(Optional.of(result), saved.expenseId());
        assertEquals(NOW, saved.resolvedAt());
    }

    @Test
    void execute_shouldPayTodayOccurrence() {
        pay(TODAY);

        assertEquals(TODAY, resolutionRepository.created.get(0).dueDate());
    }

    @Test
    void execute_shouldPayFutureScheduledDate() {
        pay(FUTURE);

        assertEquals(FUTURE, resolutionRepository.created.get(0).dueDate());
        assertEquals(1, expenseRepository.created.size());
    }

    @Test
    void execute_shouldRecordSuppliedAmountAsIsWhenItDiffersFromExpected() {
        pay(OVERDUE, Money.ofCents(12345), null, null, null);

        assertEquals(Money.ofCents(12345), expenseRepository.created.get(0).amount());
        assertEquals(Money.ofCents(50000 - 12345), account.balance());
        assertEquals(ResolutionStatus.PAID, resolutionRepository.created.get(0).status());
    }

    @Test
    void execute_shouldUseSuppliedPastOperationDate() {
        pay(OVERDUE, null, d(2026, 8, 13), null, null);

        assertEquals(d(2026, 8, 13), expenseRepository.created.get(0).operationDate());
        assertEquals(NOW, resolutionRepository.created.get(0).resolvedAt());
    }

    @Test
    void execute_shouldAcceptOperationDateEqualToToday() {
        pay(OVERDUE, null, TODAY, null, null);

        assertEquals(TODAY, expenseRepository.created.get(0).operationDate());
    }

    @Test
    void execute_shouldRejectOperationDateAfterTodayBeforeAnyRepositoryCall() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> pay(OVERDUE, null, d(2026, 8, 21), null, null));

        assertEquals("Operation date cannot be after today", e.getMessage());
        assertEquals(0, obligationRepository.findCalls);
        assertEquals(0, resolutionRepository.findByDateCalls);
        assertEquals(0, accountRepository.findCalls);
        assertEquals(0, categoryRepository.findCalls);
        assertNothingPersisted();
    }

    @Test
    void execute_shouldRejectFutureOperationDateEvenWhenObligationDoesNotExist() {
        PayOccurrenceCommand command = new PayOccurrenceCommand(userId, UUID.randomUUID(), OVERDUE, null,
            d(2026, 8, 21), null, null);
        PayOccurrence useCase = useCase();

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }

    @Test
    void execute_shouldUseAlternativeAccountOnly() {
        Account alternative = otherAccount(userId);

        pay(OVERDUE, null, null, alternative.id(), null);

        Expense expense = expenseRepository.created.get(0);
        assertEquals(alternative.id(), expense.accountId());
        assertEquals(category.id(), expense.categoryId());
        assertEquals(Money.ofCents(40000), alternative.balance());
        assertEquals(Money.ofCents(50000), account.balance());
    }

    @Test
    void execute_shouldUseAlternativeCategoryOnly() {
        Category alternative = otherCategory(userId, CategoryType.EXPENSE);

        pay(OVERDUE, null, null, null, alternative.id());

        Expense expense = expenseRepository.created.get(0);
        assertEquals(account.id(), expense.accountId());
        assertEquals(alternative.id(), expense.categoryId());
    }

    @Test
    void execute_shouldUseAlternativeAccountAndCategory() {
        Account alternativeAccount = otherAccount(userId);
        Category alternativeCategory = otherCategory(userId, CategoryType.EXPENSE);

        pay(OVERDUE, null, null, alternativeAccount.id(), alternativeCategory.id());

        Expense expense = expenseRepository.created.get(0);
        assertEquals(alternativeAccount.id(), expense.accountId());
        assertEquals(alternativeCategory.id(), expense.categoryId());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenAlternativeAccountBelongsToAnotherUser() {
        Account foreign = otherAccount(UUID.randomUUID());

        assertThrows(ResourceNotFoundException.class, () -> pay(OVERDUE, null, null, foreign.id(), null));

        assertNothingPersisted();
        assertEquals(Money.ofCents(50000), foreign.balance());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenAlternativeCategoryBelongsToAnotherUser() {
        Category foreign = otherCategory(UUID.randomUUID(), CategoryType.EXPENSE);

        assertThrows(ResourceNotFoundException.class, () -> pay(OVERDUE, null, null, null, foreign.id()));

        assertNothingPersisted();
    }

    @Test
    void execute_shouldRejectInactiveObligationAccountWithoutAlternative() {
        account.deactivate();

        assertRule(BusinessRuleCode.ACCOUNT_INACTIVE, "Account must be active", () -> pay(OVERDUE));

        assertNothingPersisted();
    }

    @Test
    void execute_shouldPayWithActiveAlternativeWhenObligationAccountIsInactive() {
        account.deactivate();
        Account alternative = otherAccount(userId);

        pay(OVERDUE, null, null, alternative.id(), null);

        assertEquals(alternative.id(), expenseRepository.created.get(0).accountId());
        assertEquals(1, resolutionRepository.created.size());
    }

    @Test
    void execute_shouldRejectInactiveObligationCategory() {
        category.deactivate();

        assertRule(BusinessRuleCode.CATEGORY_INACTIVE, "Category must be active", () -> pay(OVERDUE));

        assertNothingPersisted();
    }

    @Test
    void execute_shouldRejectAlternativeCategoryOfWrongType() {
        Category income = otherCategory(userId, CategoryType.INCOME);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> pay(OVERDUE, null, null, null, income.id()));

        assertEquals("Category type is not valid for an expense", e.getMessage());
        assertNothingPersisted();
    }

    @Test
    void execute_shouldRejectInsufficientBalance() {
        Account poor = otherAccount(userId);
        poor.debit(Money.ofCents(40001));

        assertRule(BusinessRuleCode.INSUFFICIENT_BALANCE, "Account balance is insufficient",
            () -> pay(OVERDUE, null, null, poor.id(), null));

        assertNothingPersisted();
        assertEquals(Money.ofCents(9999), poor.balance());
    }

    @Test
    void execute_shouldPayWhenBalanceEqualsAmount() {
        Account exact = otherAccount(userId);
        exact.debit(Money.ofCents(40000));

        pay(OVERDUE, null, null, exact.id(), null);

        assertEquals(Money.ofCents(0), exact.balance());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenObligationBelongsToAnotherUser() {
        PayOccurrenceCommand command = new PayOccurrenceCommand(UUID.randomUUID(), obligation.id(), OVERDUE, null,
            null, null, null);
        PayOccurrence useCase = useCase();

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(command));

        assertEquals(0, resolutionRepository.findByDateCalls);
        assertEquals(0, accountRepository.findCalls);
        assertNothingPersisted();
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenObligationDoesNotExist() {
        PayOccurrenceCommand command = new PayOccurrenceCommand(userId, UUID.randomUUID(), OVERDUE, null, null,
            null, null);
        PayOccurrence useCase = useCase();

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(command));

        assertNothingPersisted();
    }

    @Test
    void execute_shouldRejectArchivedObligationWithoutResolutionLookup() {
        obligation = obligation(ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived", () -> pay(OVERDUE));

        assertEquals(0, resolutionRepository.findByDateCalls);
        assertEquals(0, accountRepository.findCalls);
        assertNothingPersisted();
    }

    @Test
    void execute_shouldRejectDateOutsideTheCalendar() {
        assertRule(BusinessRuleCode.OCCURRENCE_NOT_SCHEDULED, "Occurrence is not scheduled for the obligation",
            () -> pay(d(2026, 8, 14)));

        assertEquals(0, resolutionRepository.findByDateCalls);
        assertNothingPersisted();
    }

    @Test
    void execute_shouldRejectAlreadyPaidOccurrence() {
        existing(OVERDUE, ResolutionStatus.PAID);

        assertRule(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED,
            ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE, () -> pay(OVERDUE));

        assertEquals(0, accountRepository.findCalls);
        assertEquals(Money.ofCents(50000), account.balance());
        assertNothingPersisted();
    }

    @Test
    void execute_shouldRejectAlreadySkippedOccurrence() {
        existing(OVERDUE, ResolutionStatus.SKIPPED);

        assertRule(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED,
            ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE, () -> pay(OVERDUE));

        assertNothingPersisted();
    }

    @Test
    void execute_shouldPayDateWhenOnlyAnotherDateIsResolved() {
        existing(OVERDUE, ResolutionStatus.SKIPPED);

        pay(TODAY);

        assertEquals(1, resolutionRepository.created.size());
    }

    @Test
    void execute_shouldCheckArchivedBeforeNotScheduled() {
        obligation = obligation(ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived", () -> pay(d(2026, 8, 14)));
    }

    @Test
    void execute_shouldCheckNotScheduledBeforeAlreadyResolved() {
        existing(d(2026, 8, 14), ResolutionStatus.SKIPPED);

        assertRule(BusinessRuleCode.OCCURRENCE_NOT_SCHEDULED, "Occurrence is not scheduled for the obligation",
            () -> pay(d(2026, 8, 14)));
    }

    @Test
    void execute_shouldCheckAlreadyResolvedBeforeAccountNotFound() {
        existing(OVERDUE, ResolutionStatus.SKIPPED);
        accountRepository.accounts.clear();

        assertRule(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED,
            ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE, () -> pay(OVERDUE));
    }

    @Test
    void execute_shouldCheckAlreadyResolvedBeforeCategoryNotFound() {
        existing(OVERDUE, ResolutionStatus.PAID);
        categoryRepository.categories.clear();

        assertRule(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED,
            ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE, () -> pay(OVERDUE));
    }

    @Test
    void execute_shouldCheckAlreadyResolvedBeforeInsufficientBalance() {
        existing(OVERDUE, ResolutionStatus.PAID);
        account.debit(Money.ofCents(50000));

        assertRule(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED,
            ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE, () -> pay(OVERDUE));
    }

    @Test
    void execute_shouldThrowWhenCommandIsNull() {
        assertThrows(NullPointerException.class, () -> useCase().execute(null));
    }

    @Test
    void constructor_shouldThrowWhenObligationRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new PayOccurrence(null,
            resolutionRepository, registration(), transactionManager, CLOCK));
        assertEquals("Obligation repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenOccurrenceResolutionRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new PayOccurrence(
            obligationRepository, null, registration(), transactionManager, CLOCK));
        assertEquals("Occurrence resolution repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenExpenseRegistrationIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new PayOccurrence(
            obligationRepository, resolutionRepository, null, transactionManager, CLOCK));
        assertEquals("Expense registration cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenTransactionManagerIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new PayOccurrence(
            obligationRepository, resolutionRepository, registration(), null, CLOCK));
        assertEquals("Transaction manager cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenClockIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new PayOccurrence(
            obligationRepository, resolutionRepository, registration(), transactionManager, null));
        assertEquals("Clock cannot be null", e.getMessage());
    }

    private ExpenseRegistration registration() {
        return new ExpenseRegistration(accountRepository, categoryRepository, expenseRepository);
    }

    private static final class RecordingTransactionManager implements TransactionManager {
        private boolean executed;
        private int executions;

        @Override
        public <T> T execute(Supplier<T> transactionalWork) {
            executed = true;
            executions++;
            return transactionalWork.get();
        }
    }

    private static final class RecordingObligationRepository implements ObligationRepository {
        private final List<Obligation> stored = new ArrayList<>();
        private int findCalls;

        @Override
        public Obligation create(Obligation obligation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Obligation> findByIdAndUserId(UUID id, UUID userId) {
            findCalls++;
            return stored.stream().filter(o -> o.id().equals(id) && o.userId().equals(userId)).findFirst();
        }

        @Override
        public boolean existsByUserIdAndName(UUID userId, String name) {
            throw new UnsupportedOperationException();
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

    private static final class RecordingResolutionRepository implements OccurrenceResolutionRepository {
        private final List<OccurrenceResolution> resolutions = new ArrayList<>();
        private final List<OccurrenceResolution> created = new ArrayList<>();
        private int findByDateCalls;

        @Override
        public OccurrenceResolution create(OccurrenceResolution resolution) {
            created.add(resolution);
            resolutions.add(resolution);
            return resolution;
        }

        @Override
        public List<OccurrenceResolution> findByObligationId(UUID obligationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OccurrenceResolution> findByObligationIdAndDueDate(UUID obligationId, LocalDate dueDate) {
            findByDateCalls++;
            return resolutions.stream()
                .filter(r -> r.obligationId().equals(obligationId) && r.dueDate().equals(dueDate)).findFirst();
        }

        @Override
        public Optional<OccurrenceResolution> findByExpenseId(UUID expenseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(UUID resolutionId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingAccountRepository implements AccountRepository {
        private final Map<UUID, Account> accounts = new HashMap<>();
        private final List<Account> updated = new ArrayList<>();
        private int findCalls;

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
            updated.add(account);
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
        private final List<Expense> created = new ArrayList<>();

        @Override
        public Expense create(Expense expense) {
            created.add(expense);
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
