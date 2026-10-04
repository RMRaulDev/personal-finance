package com.rauldev.personalfinance.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
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
import com.rauldev.personalfinance.domain.OccurrenceResolution;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.domain.ResolutionStatus;

class ModifyObligationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final Money ORIGINAL_AMOUNT = Money.ofCents(100000);
    private static final Recurrence UPCOMING = new Recurrence(Frequency.MONTHLY, LocalDate.of(2026, 9, 1), null);
    private static final Recurrence OVERDUE = new Recurrence(Frequency.MONTHLY, LocalDate.of(2026, 8, 1), null);

    private final UUID userId = UUID.randomUUID();
    private final Account account = new Account(UUID.randomUUID(), userId, "Checking");
    private final Category category = new Category(UUID.randomUUID(), userId, "Rent", CategoryType.EXPENSE);
    private final Account newAccount = new Account(UUID.randomUUID(), userId, "Savings");
    private final Category newCategory = new Category(UUID.randomUUID(), userId, "Utilities", CategoryType.EXPENSE);
    private final RecordingObligationRepository obligationRepository = new RecordingObligationRepository();
    private final RecordingResolutionRepository resolutionRepository = new RecordingResolutionRepository();
    private final RecordingAccountRepository accountRepository = new RecordingAccountRepository(account, newAccount);
    private final RecordingCategoryRepository categoryRepository =
        new RecordingCategoryRepository(category, newCategory);
    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();

    private static Clock clockAt(LocalDate date) {
        return Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    private ModifyObligation useCase() {
        return useCase(clockAt(TODAY));
    }

    private ModifyObligation useCase(Clock clock) {
        return new ModifyObligation(obligationRepository, resolutionRepository, accountRepository,
            categoryRepository, transactionManager, clock);
    }

    private Obligation seed(Recurrence recurrence, ObligationStatus status) {
        return seed("Rent", recurrence, status);
    }

    private Obligation seed(String name, Recurrence recurrence, ObligationStatus status) {
        Obligation obligation = new Obligation(UUID.randomUUID(), userId, name, ORIGINAL_AMOUNT, account.id(),
            category.id(), recurrence, status);
        obligationRepository.stored.put(obligation.id(), obligation);
        return obligation;
    }

    private Obligation seed() {
        return seed(UPCOMING, ObligationStatus.ACTIVE);
    }

    private ModifyObligationCommand rename(Obligation obligation, String name) {
        return new ModifyObligationCommand(userId, obligation.id(), name, null, null, null, null);
    }

    private ModifyObligationCommand amount(Obligation obligation, Money amount) {
        return new ModifyObligationCommand(userId, obligation.id(), null, amount, null, null, null);
    }

    private ModifyObligationCommand account(Obligation obligation, UUID accountId) {
        return new ModifyObligationCommand(userId, obligation.id(), null, null, accountId, null, null);
    }

    private ModifyObligationCommand category(Obligation obligation, UUID categoryId) {
        return new ModifyObligationCommand(userId, obligation.id(), null, null, null, categoryId, null);
    }

    private ModifyObligationCommand recurrence(Obligation obligation, Recurrence recurrence) {
        return new ModifyObligationCommand(userId, obligation.id(), null, null, null, null, recurrence);
    }

    private Obligation persisted(Obligation obligation) {
        return obligationRepository.stored.get(obligation.id());
    }

    private static OccurrenceResolution skipped(Obligation obligation, LocalDate dueDate) {
        return new OccurrenceResolution(UUID.randomUUID(), obligation.id(), dueDate, ResolutionStatus.SKIPPED, null,
            Instant.parse("2026-08-01T00:00:00Z"));
    }

    private static BusinessRuleViolationException assertRule(BusinessRuleCode code, Runnable action) {
        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class, action::run);
        assertEquals(code, e.code());
        return e;
    }

    @Test
    void constructor_shouldThrowWhenObligationRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new ModifyObligation(
            null, resolutionRepository, accountRepository, categoryRepository, transactionManager, clockAt(TODAY)));
        assertEquals("Obligation repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenOccurrenceResolutionRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new ModifyObligation(
            obligationRepository, null, accountRepository, categoryRepository, transactionManager, clockAt(TODAY)));
        assertEquals("Occurrence resolution repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenAccountRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new ModifyObligation(
            obligationRepository, resolutionRepository, null, categoryRepository, transactionManager,
            clockAt(TODAY)));
        assertEquals("Account repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenCategoryRepositoryIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new ModifyObligation(
            obligationRepository, resolutionRepository, accountRepository, null, transactionManager,
            clockAt(TODAY)));
        assertEquals("Category repository cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenTransactionManagerIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new ModifyObligation(
            obligationRepository, resolutionRepository, accountRepository, categoryRepository, null,
            clockAt(TODAY)));
        assertEquals("Transaction manager cannot be null", e.getMessage());
    }

    @Test
    void constructor_shouldThrowWhenClockIsNull() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new ModifyObligation(
            obligationRepository, resolutionRepository, accountRepository, categoryRepository, transactionManager,
            null));
        assertEquals("Clock cannot be null", e.getMessage());
    }

    @Test
    void execute_shouldThrowWhenCommandIsNull() {
        assertThrows(NullPointerException.class, () -> useCase().execute(null));
    }

    @Test
    void execute_shouldRenameObligationAndPersistItOnce() {
        Obligation obligation = seed();

        UUID result = useCase().execute(rename(obligation, "Mortgage"));

        assertEquals(obligation.id(), result);
        assertTrue(transactionManager.executed);
        assertEquals(1, obligationRepository.updateCalls);
        assertEquals("Mortgage", persisted(obligation).name());
        assertEquals(ORIGINAL_AMOUNT, persisted(obligation).amount());
        assertEquals(account.id(), persisted(obligation).accountId());
        assertEquals(category.id(), persisted(obligation).categoryId());
        assertEquals(UPCOMING, persisted(obligation).recurrence());
        assertEquals(ObligationStatus.ACTIVE, persisted(obligation).status());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenObligationDoesNotExist() {
        Obligation obligation = seed();
        ModifyObligationCommand command = new ModifyObligationCommand(
            userId, UUID.randomUUID(), "Mortgage", null, newAccount.id(), null, null);

        assertThrows(ResourceNotFoundException.class, () -> useCase().execute(command));

        assertEquals(0, accountRepository.findCalls);
        assertEquals(0, categoryRepository.findCalls);
        assertEquals(0, resolutionRepository.findCalls);
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals("Rent", persisted(obligation).name());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenObligationBelongsToAnotherUser() {
        Obligation obligation = seed();
        ModifyObligationCommand command = new ModifyObligationCommand(
            UUID.randomUUID(), obligation.id(), "Mortgage", null, null, null, null);

        assertThrows(ResourceNotFoundException.class, () -> useCase().execute(command));

        assertEquals(0, accountRepository.findCalls);
        assertEquals(0, categoryRepository.findCalls);
        assertEquals(0, resolutionRepository.findCalls);
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals("Rent", persisted(obligation).name());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenNewAccountDoesNotExist() {
        Obligation obligation = seed();

        assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(account(obligation, UUID.randomUUID())));

        assertEquals(0, resolutionRepository.findCalls);
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(account.id(), persisted(obligation).accountId());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenNewAccountBelongsToAnotherUser() {
        Obligation obligation = seed();
        Account foreign = new Account(UUID.randomUUID(), UUID.randomUUID(), "Foreign");
        accountRepository.accounts.add(foreign);

        assertThrows(ResourceNotFoundException.class, () -> useCase().execute(account(obligation, foreign.id())));

        assertEquals(0, resolutionRepository.findCalls);
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(account.id(), persisted(obligation).accountId());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenNewCategoryDoesNotExist() {
        Obligation obligation = seed();

        assertThrows(ResourceNotFoundException.class,
            () -> useCase().execute(category(obligation, UUID.randomUUID())));

        assertEquals(0, resolutionRepository.findCalls);
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(category.id(), persisted(obligation).categoryId());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenNewCategoryBelongsToAnotherUser() {
        Obligation obligation = seed();
        Category foreign = new Category(UUID.randomUUID(), UUID.randomUUID(), "Foreign", CategoryType.EXPENSE);
        categoryRepository.categories.add(foreign);

        assertThrows(ResourceNotFoundException.class, () -> useCase().execute(category(obligation, foreign.id())));

        assertEquals(0, resolutionRepository.findCalls);
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(category.id(), persisted(obligation).categoryId());
    }

    @Test
    void execute_shouldThrowBusinessRuleViolationExceptionWhenAnotherObligationUsesSameName() {
        Obligation obligation = seed();
        seed("Mortgage", UPCOMING, ObligationStatus.ACTIVE);

        BusinessRuleViolationException e = assertRule(BusinessRuleCode.OBLIGATION_NAME_ALREADY_EXISTS,
            () -> useCase().execute(rename(obligation, "Mortgage")));

        assertEquals(ApplicationConstants.OBLIGATION_NAME_ALREADY_EXISTS_MESSAGE, e.getMessage());
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals("Rent", persisted(obligation).name());
    }

    @Test
    void execute_shouldAllowObligationToKeepItsOwnName() {
        Obligation obligation = seed();

        UUID result = useCase().execute(rename(obligation, "Rent"));

        assertEquals(obligation.id(), result);
        assertEquals(obligation.id(), obligationRepository.lastExcludedId);
        assertEquals(1, obligationRepository.updateCalls);
        assertEquals("Rent", persisted(obligation).name());
    }

    @Test
    void execute_shouldNotRejectRenameWhenAnotherUsersObligationUsesSameName() {
        Obligation obligation = seed();
        Obligation foreign = new Obligation(UUID.randomUUID(), UUID.randomUUID(), "Mortgage", ORIGINAL_AMOUNT,
            UUID.randomUUID(), UUID.randomUUID(), UPCOMING, ObligationStatus.ACTIVE);
        obligationRepository.stored.put(foreign.id(), foreign);

        UUID result = useCase().execute(rename(obligation, "Mortgage"));

        assertEquals(obligation.id(), result);
        assertEquals("Mortgage", persisted(obligation).name());
        assertEquals(1, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldAllowRenameWhenObligationHasOverdueOccurrences() {
        Obligation obligation = seed(OVERDUE, ObligationStatus.ACTIVE);

        useCase().execute(rename(obligation, "Mortgage"));

        assertEquals("Mortgage", persisted(obligation).name());
        assertEquals(1, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenNewNameIsBlank() {
        Obligation obligation = seed();

        assertThrows(IllegalArgumentException.class, () -> useCase().execute(rename(obligation, " ")));

        assertEquals(0, obligationRepository.updateCalls);
        assertEquals("Rent", persisted(obligation).name());
    }

    @Test
    void execute_shouldThrowObligationArchivedBeforeDuplicateNameWhenArchived() {
        Obligation obligation = seed(UPCOMING, ObligationStatus.ARCHIVED);
        seed("Mortgage", UPCOMING, ObligationStatus.ACTIVE);

        BusinessRuleViolationException e = assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED,
            () -> useCase().execute(rename(obligation, "Mortgage")));

        assertEquals("Obligation is archived", e.getMessage());
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals("Rent", persisted(obligation).name());
    }

    @Test
    void execute_shouldChangeAmountAndPersistIt() {
        Obligation obligation = seed();

        useCase().execute(amount(obligation, Money.ofCents(1)));

        assertEquals(Money.ofCents(1), persisted(obligation).amount());
        assertEquals("Rent", persisted(obligation).name());
        assertEquals(1, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenNewAmountIsZero() {
        Obligation obligation = seed();

        assertThrows(IllegalArgumentException.class, () -> useCase().execute(amount(obligation, Money.ofCents(0))));

        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(ORIGINAL_AMOUNT, persisted(obligation).amount());
    }

    @Test
    void execute_shouldThrowWhenChangingAmountWithOverdueOccurrences() {
        Obligation obligation = seed(OVERDUE, ObligationStatus.ACTIVE);

        BusinessRuleViolationException e = assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES,
            () -> useCase().execute(amount(obligation, Money.ofCents(1))));

        assertEquals("Obligation has overdue occurrences", e.getMessage());
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(ORIGINAL_AMOUNT, persisted(obligation).amount());
    }

    @Test
    void execute_shouldThrowWhenSendingUnchangedAmountWithOverdueOccurrences() {
        Obligation obligation = seed(OVERDUE, ObligationStatus.ACTIVE);

        BusinessRuleViolationException e = assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES,
            () -> useCase().execute(amount(obligation, ORIGINAL_AMOUNT)));

        assertEquals("Obligation has overdue occurrences", e.getMessage());
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(ORIGINAL_AMOUNT, persisted(obligation).amount());
    }

    @Test
    void execute_shouldAllowChangingAmountWhenOverdueOccurrencesAreResolved() {
        Obligation obligation = seed(OVERDUE, ObligationStatus.ACTIVE);
        resolutionRepository.resolutions.add(skipped(obligation, LocalDate.of(2026, 8, 1)));

        useCase().execute(amount(obligation, Money.ofCents(1)));

        assertEquals(1, resolutionRepository.findCalls);
        assertEquals(Money.ofCents(1), persisted(obligation).amount());
    }

    @Test
    void execute_shouldThrowObligationArchivedWhenChangingAmountOfArchivedObligation() {
        Obligation obligation = seed(UPCOMING, ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED,
            () -> useCase().execute(amount(obligation, Money.ofCents(1))));

        assertEquals(0, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldChangeAccountAndKeepCurrentCategory() {
        Obligation obligation = seed();

        useCase().execute(account(obligation, newAccount.id()));

        assertEquals(newAccount.id(), persisted(obligation).accountId());
        assertEquals(category.id(), persisted(obligation).categoryId());
        assertEquals(1, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldChangeCategoryAndKeepCurrentAccount() {
        Obligation obligation = seed();

        useCase().execute(category(obligation, newCategory.id()));

        assertEquals(account.id(), persisted(obligation).accountId());
        assertEquals(newCategory.id(), persisted(obligation).categoryId());
        assertEquals(1, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowAccountInactiveWhenNewAccountIsInactive() {
        Obligation obligation = seed();
        newAccount.deactivate();

        BusinessRuleViolationException e = assertRule(BusinessRuleCode.ACCOUNT_INACTIVE,
            () -> useCase().execute(account(obligation, newAccount.id())));

        assertEquals("Account must be active", e.getMessage());
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(account.id(), persisted(obligation).accountId());
    }

    @Test
    void execute_shouldThrowAccountInactiveWhenOnlyCategoryChangesAndCurrentAccountIsInactive() {
        Obligation obligation = seed();
        account.deactivate();

        BusinessRuleViolationException e = assertRule(BusinessRuleCode.ACCOUNT_INACTIVE,
            () -> useCase().execute(category(obligation, newCategory.id())));

        assertEquals("Account must be active", e.getMessage());
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(category.id(), persisted(obligation).categoryId());
    }

    @Test
    void execute_shouldThrowCategoryInactiveWhenNewCategoryIsInactive() {
        Obligation obligation = seed();
        newCategory.deactivate();

        BusinessRuleViolationException e = assertRule(BusinessRuleCode.CATEGORY_INACTIVE,
            () -> useCase().execute(category(obligation, newCategory.id())));

        assertEquals("Category must be active", e.getMessage());
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(category.id(), persisted(obligation).categoryId());
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenNewCategoryIsIncome() {
        Obligation obligation = seed();
        Category income = new Category(UUID.randomUUID(), userId, "Salary", CategoryType.INCOME);
        categoryRepository.categories.add(income);

        assertThrows(IllegalArgumentException.class, () -> useCase().execute(category(obligation, income.id())));

        assertEquals(0, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowWhenChangingPaymentSourceWithOverdueOccurrences() {
        Obligation obligation = seed(OVERDUE, ObligationStatus.ACTIVE);

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES,
            () -> useCase().execute(account(obligation, newAccount.id())));

        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(account.id(), persisted(obligation).accountId());
    }

    @Test
    void execute_shouldThrowObligationArchivedWhenChangingPaymentSourceOfArchivedObligation() {
        Obligation obligation = seed(UPCOMING, ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED,
            () -> useCase().execute(category(obligation, newCategory.id())));

        assertEquals(0, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldChangeRecurrenceAndPersistIt() {
        Obligation obligation = seed();
        Recurrence withEnd = new Recurrence(Frequency.MONTHLY, UPCOMING.startDate(), LocalDate.of(2026, 12, 1));

        useCase().execute(recurrence(obligation, withEnd));

        assertEquals(withEnd, persisted(obligation).recurrence());
        assertEquals(1, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowWhenChangingRecurrenceWithOverdueOccurrences() {
        Obligation obligation = seed(OVERDUE, ObligationStatus.ACTIVE);
        Recurrence withEnd = new Recurrence(Frequency.MONTHLY, OVERDUE.startDate(), LocalDate.of(2026, 12, 1));

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES,
            () -> useCase().execute(recurrence(obligation, withEnd)));

        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(OVERDUE, persisted(obligation).recurrence());
    }

    @Test
    void execute_shouldThrowObligationArchivedWhenChangingRecurrenceOfArchivedObligation() {
        Obligation obligation = seed(UPCOMING, ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED,
            () -> useCase().execute(recurrence(obligation, new Recurrence(Frequency.WEEKLY,
                LocalDate.of(2026, 9, 1), null))));

        assertEquals(0, obligationRepository.updateCalls);
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenNewRecurrenceStartsBeforeToday() {
        Obligation obligation = seed();
        Recurrence past = new Recurrence(Frequency.MONTHLY, TODAY.minusDays(1), null);

        assertThrows(IllegalArgumentException.class, () -> useCase().execute(recurrence(obligation, past)));

        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(UPCOMING, persisted(obligation).recurrence());
    }

    @Test
    void execute_shouldAcceptNewRecurrenceStartingToday() {
        Obligation obligation = seed();
        Recurrence startingToday = new Recurrence(Frequency.MONTHLY, TODAY, null);

        useCase().execute(recurrence(obligation, startingToday));

        assertEquals(startingToday, persisted(obligation).recurrence());
    }

    @Test
    void execute_shouldThrowWhenNewRecurrenceConflictsWithPersistedUpcomingResolution() {
        Obligation obligation = seed();
        resolutionRepository.resolutions.add(skipped(obligation, LocalDate.of(2026, 9, 1)));
        Recurrence later = new Recurrence(Frequency.MONTHLY, LocalDate.of(2026, 9, 15), null);

        assertRule(BusinessRuleCode.OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION,
            () -> useCase().execute(recurrence(obligation, later)));

        assertEquals(1, resolutionRepository.findCalls);
        assertEquals(0, obligationRepository.updateCalls);
        assertEquals(UPCOMING, persisted(obligation).recurrence());
    }

    @Test
    void execute_shouldNotPersistAnyChangeWhenRecurrenceIsRejectedAfterValidRename() {
        Obligation obligation = seed();
        Recurrence past = new Recurrence(Frequency.MONTHLY, TODAY.minusDays(1), null);

        assertThrows(IllegalArgumentException.class, () -> useCase().execute(new ModifyObligationCommand(
            userId, obligation.id(), "Mortgage", Money.ofCents(1), null, null, past)));

        assertEquals(0, obligationRepository.updateCalls);
        assertEquals("Rent", persisted(obligation).name());
        assertEquals(ORIGINAL_AMOUNT, persisted(obligation).amount());
        assertEquals(UPCOMING, persisted(obligation).recurrence());
    }

    @Test
    void execute_shouldNotPersistAnyChangeWhenNewAccountIsInactiveAfterValidRename() {
        Obligation obligation = seed();
        newAccount.deactivate();

        assertRule(BusinessRuleCode.ACCOUNT_INACTIVE, () -> useCase().execute(new ModifyObligationCommand(
            userId, obligation.id(), "Mortgage", Money.ofCents(1), newAccount.id(), null, null)));

        assertEquals(0, obligationRepository.updateCalls);
        assertEquals("Rent", persisted(obligation).name());
        assertEquals(ORIGINAL_AMOUNT, persisted(obligation).amount());
        assertEquals(account.id(), persisted(obligation).accountId());
    }

    @Test
    void execute_shouldApplyEveryRequestedChangeAndPersistOnce() {
        Obligation obligation = seed();
        Recurrence weekly = new Recurrence(Frequency.WEEKLY, LocalDate.of(2026, 9, 3), null);

        useCase().execute(new ModifyObligationCommand(userId, obligation.id(), "Mortgage", Money.ofCents(5),
            newAccount.id(), newCategory.id(), weekly));

        Obligation saved = persisted(obligation);
        assertEquals(1, obligationRepository.updateCalls);
        assertEquals("Mortgage", saved.name());
        assertEquals(Money.ofCents(5), saved.amount());
        assertEquals(newAccount.id(), saved.accountId());
        assertEquals(newCategory.id(), saved.categoryId());
        assertEquals(weekly, saved.recurrence());
    }

    @Test
    void execute_shouldAllowAmountChangeWhenClockIsOnFirstDueDate() {
        Obligation obligation = seed(OVERDUE, ObligationStatus.ACTIVE);

        useCase(clockAt(OVERDUE.startDate())).execute(amount(obligation, Money.ofCents(1)));

        assertEquals(Money.ofCents(1), persisted(obligation).amount());
    }

    @Test
    void execute_shouldRejectAmountChangeWhenClockIsOneDayAfterFirstDueDate() {
        Obligation obligation = seed(OVERDUE, ObligationStatus.ACTIVE);

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES,
            () -> useCase(clockAt(OVERDUE.startDate().plusDays(1))).execute(amount(obligation, Money.ofCents(1))));

        assertEquals(ORIGINAL_AMOUNT, persisted(obligation).amount());
    }

    private static final class RecordingTransactionManager implements TransactionManager {
        private boolean executed;

        @Override
        public <T> T execute(Supplier<T> transactionalWork) {
            executed = true;
            return transactionalWork.get();
        }
    }

    /** Hands out copies, so a rejected change leaves the stored state untouched like a rolled back transaction. */
    private static final class RecordingObligationRepository implements ObligationRepository {
        private final Map<UUID, Obligation> stored = new LinkedHashMap<>();
        private int updateCalls;
        private UUID lastExcludedId;

        private static Obligation copy(Obligation o) {
            return new Obligation(o.id(), o.userId(), o.name(), o.amount(), o.accountId(), o.categoryId(),
                o.recurrence(), o.status());
        }

        @Override
        public Obligation create(Obligation obligation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Obligation> findByIdAndUserId(UUID id, UUID userId) {
            return Optional.ofNullable(stored.get(id))
                .filter(o -> o.userId().equals(userId))
                .map(RecordingObligationRepository::copy);
        }

        @Override
        public boolean existsByUserIdAndName(UUID userId, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID obligationId) {
            lastExcludedId = obligationId;
            return stored.values().stream().anyMatch(
                o -> o.userId().equals(userId) && o.name().equals(name) && !o.id().equals(obligationId));
        }

        @Override
        public Obligation update(Obligation obligation) {
            updateCalls++;
            stored.put(obligation.id(), copy(obligation));
            return obligation;
        }
    }

    private static final class RecordingResolutionRepository implements OccurrenceResolutionRepository {
        private final List<OccurrenceResolution> resolutions = new ArrayList<>();
        private int findCalls;

        @Override
        public OccurrenceResolution create(OccurrenceResolution resolution) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OccurrenceResolution> findByObligationId(UUID obligationId) {
            findCalls++;
            return resolutions.stream().filter(r -> r.obligationId().equals(obligationId)).toList();
        }

        @Override
        public Optional<OccurrenceResolution> findByObligationIdAndDueDate(UUID obligationId, LocalDate dueDate) {
            throw new UnsupportedOperationException();
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
        private final List<Account> accounts = new ArrayList<>();
        private int findCalls;

        private RecordingAccountRepository(Account... accounts) {
            this.accounts.addAll(List.of(accounts));
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

        private RecordingCategoryRepository(Category... categories) {
            this.categories.addAll(List.of(categories));
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
}
