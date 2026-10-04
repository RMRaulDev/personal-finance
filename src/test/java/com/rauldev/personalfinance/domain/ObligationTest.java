package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ObligationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final Instant RESOLVED_AT = Instant.parse("2026-08-20T12:00:00Z");
    private static final Money AMOUNT = Money.ofCents(10000);

    private final UUID userId = UUID.randomUUID();
    private final Account account = new Account(userId, "Checking");
    private final Category category = new Category(userId, "Rent", CategoryType.EXPENSE);

    private static LocalDate d(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    private static Recurrence weekly(LocalDate start) {
        return new Recurrence(Frequency.WEEKLY, start, null);
    }

    private static Recurrence monthly(LocalDate start) {
        return new Recurrence(Frequency.MONTHLY, start, null);
    }

    private Obligation obligation(Recurrence recurrence) {
        return new Obligation(UUID.randomUUID(), userId, "Rent", AMOUNT, account.id(), category.id(), recurrence,
            ObligationStatus.ACTIVE);
    }

    private static OccurrenceResolution skipped(Obligation obligation, LocalDate dueDate) {
        return new OccurrenceResolution(UUID.randomUUID(), obligation.id(), dueDate, ResolutionStatus.SKIPPED, null,
            RESOLVED_AT);
    }

    private static OccurrenceResolution paid(Obligation obligation, LocalDate dueDate) {
        return new OccurrenceResolution(UUID.randomUUID(), obligation.id(), dueDate, ResolutionStatus.PAID,
            UUID.randomUUID(), RESOLVED_AT);
    }

    /** Weekly obligation with two overdue dates (2026-08-06 and 2026-08-13) as of TODAY. */
    private Obligation overdueObligation() {
        return obligation(weekly(d(2026, 8, 6)));
    }

    private static void assertRule(BusinessRuleCode code, String message, Runnable action) {
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, action::run);
        assertEquals(code, exception.code());
        assertEquals(message, exception.getMessage());
    }

    // Constructor

    @Test
    void constructorRejectsNullFields() {
        Recurrence recurrence = weekly(TODAY);
        UUID id = UUID.randomUUID();
        ObligationStatus active = ObligationStatus.ACTIVE;

        assertThrows(NullPointerException.class,
            () -> new Obligation(null, userId, "Rent", AMOUNT, account.id(), category.id(), recurrence, active));
        assertThrows(NullPointerException.class,
            () -> new Obligation(id, null, "Rent", AMOUNT, account.id(), category.id(), recurrence, active));
        assertThrows(NullPointerException.class,
            () -> new Obligation(id, userId, null, AMOUNT, account.id(), category.id(), recurrence, active));
        assertThrows(NullPointerException.class,
            () -> new Obligation(id, userId, "Rent", null, account.id(), category.id(), recurrence, active));
        assertThrows(NullPointerException.class,
            () -> new Obligation(id, userId, "Rent", AMOUNT, null, category.id(), recurrence, active));
        assertThrows(NullPointerException.class,
            () -> new Obligation(id, userId, "Rent", AMOUNT, account.id(), null, recurrence, active));
        assertThrows(NullPointerException.class,
            () -> new Obligation(id, userId, "Rent", AMOUNT, account.id(), category.id(), null, active));
        assertThrows(NullPointerException.class,
            () -> new Obligation(id, userId, "Rent", AMOUNT, account.id(), category.id(), recurrence, null));
    }

    @Test
    void constructorRejectsBlankName() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Obligation(UUID.randomUUID(), userId, "  ", AMOUNT, account.id(), category.id(),
                weekly(TODAY), ObligationStatus.ACTIVE));

        assertEquals("Obligation name cannot be empty", exception.getMessage());
    }

    @Test
    void constructorRejectsZeroAmount() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Obligation(UUID.randomUUID(), userId, "Rent", Money.ofCents(0), account.id(), category.id(),
                weekly(TODAY), ObligationStatus.ACTIVE));

        assertEquals("Obligation amount must be greater than zero", exception.getMessage());
    }

    @Test
    void constructorAllowsInactiveReferencesAndArchivedStatus() {
        account.deactivate();
        category.deactivate();

        Obligation obligation = new Obligation(UUID.randomUUID(), userId, "Rent", AMOUNT, account.id(),
            category.id(), weekly(TODAY), ObligationStatus.ARCHIVED);

        assertEquals(ObligationStatus.ARCHIVED, obligation.status());
        assertEquals(account.id(), obligation.accountId());
        assertEquals(category.id(), obligation.categoryId());
    }

    // create

    @Test
    void createBuildsActiveObligationOwnedByTheAccountUser() {
        Recurrence recurrence = monthly(TODAY);

        Obligation obligation = Obligation.create(account, category, "Rent", AMOUNT, recurrence, TODAY);

        assertNotNull(obligation.id());
        assertEquals(userId, obligation.userId());
        assertEquals("Rent", obligation.name());
        assertEquals(AMOUNT, obligation.amount());
        assertEquals(account.id(), obligation.accountId());
        assertEquals(category.id(), obligation.categoryId());
        assertEquals(recurrence, obligation.recurrence());
        assertEquals(ObligationStatus.ACTIVE, obligation.status());
        assertNotEquals(obligation.id(), Obligation.create(account, category, "Rent", AMOUNT, recurrence, TODAY).id());
    }

    @Test
    void createRejectsAccountAndCategoryOfDifferentUsers() {
        Category foreign = new Category(UUID.randomUUID(), "Rent", CategoryType.EXPENSE);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> Obligation.create(account, foreign, "Rent", AMOUNT, monthly(TODAY), TODAY));

        assertEquals("Account and category must belong to the same user", exception.getMessage());
    }

    @Test
    void createRejectsIncomeCategory() {
        Category income = new Category(userId, "Salary", CategoryType.INCOME);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> Obligation.create(account, income, "Rent", AMOUNT, monthly(TODAY), TODAY));

        assertEquals("Category type is not valid for an obligation", exception.getMessage());
    }

    @Test
    void createChecksSameUserBeforeType() {
        Category foreignIncome = new Category(UUID.randomUUID(), "Salary", CategoryType.INCOME);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> Obligation.create(account, foreignIncome, "Rent", AMOUNT, monthly(TODAY), TODAY));

        assertEquals("Account and category must belong to the same user", exception.getMessage());
    }

    @Test
    void createChecksTypeBeforeInactiveAccount() {
        Category income = new Category(userId, "Salary", CategoryType.INCOME);
        account.deactivate();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> Obligation.create(account, income, "Rent", AMOUNT, monthly(TODAY), TODAY));

        assertEquals("Category type is not valid for an obligation", exception.getMessage());
    }

    @Test
    void createRejectsInactiveAccount() {
        account.deactivate();

        assertRule(BusinessRuleCode.ACCOUNT_INACTIVE, "Account must be active",
            () -> Obligation.create(account, category, "Rent", AMOUNT, monthly(TODAY), TODAY));
    }

    @Test
    void createRejectsInactiveCategory() {
        category.deactivate();

        assertRule(BusinessRuleCode.CATEGORY_INACTIVE, "Category must be active",
            () -> Obligation.create(account, category, "Rent", AMOUNT, monthly(TODAY), TODAY));
    }

    @Test
    void createReportsInactiveAccountWhenBothAreInactive() {
        account.deactivate();
        category.deactivate();

        assertRule(BusinessRuleCode.ACCOUNT_INACTIVE, "Account must be active",
            () -> Obligation.create(account, category, "Rent", AMOUNT, monthly(TODAY), TODAY));
    }

    @Test
    void createAcceptsStartDate31DaysBeforeToday() {
        Obligation obligation = Obligation.create(account, category, "Rent", AMOUNT,
            monthly(TODAY.minusDays(31)), TODAY);

        assertEquals(TODAY.minusDays(31), obligation.recurrence().startDate());
    }

    @Test
    void createRejectsStartDate32DaysBeforeToday() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> Obligation.create(account, category, "Rent", AMOUNT, monthly(TODAY.minusDays(32)), TODAY));

        assertEquals("Start date cannot be more than 31 days before today", exception.getMessage());
    }

    @Test
    void createRejectsBlankNameAndNonPositiveAmount() {
        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
            () -> Obligation.create(account, category, " ", AMOUNT, monthly(TODAY), TODAY));
        IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
            () -> Obligation.create(account, category, "Rent", Money.ofCents(0), monthly(TODAY), TODAY));

        assertEquals("Obligation name cannot be empty", blank.getMessage());
        assertEquals("Obligation amount must be greater than zero", zero.getMessage());
    }

    // rename

    @Test
    void renameChangesTheNameEvenWithOverdueOccurrences() {
        Obligation obligation = overdueObligation();

        obligation.rename("Mortgage");

        assertEquals("Mortgage", obligation.name());
    }

    @Test
    void renameRejectsArchivedObligation() {
        Obligation obligation = obligation(weekly(TODAY));
        obligation.archive(TODAY, List.of());

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived", () -> obligation.rename("Other"));
        assertEquals("Rent", obligation.name());
    }

    @Test
    void renameRejectsBlankName() {
        Obligation obligation = obligation(weekly(TODAY));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.rename(" "));

        assertEquals("Obligation name cannot be empty", exception.getMessage());
        assertEquals("Rent", obligation.name());
    }

    // changeAmount

    @Test
    void changeAmountUpdatesTheAmount() {
        Obligation obligation = obligation(weekly(TODAY));

        obligation.changeAmount(Money.ofCents(500), TODAY, List.of());

        assertEquals(Money.ofCents(500), obligation.amount());
    }

    @Test
    void changeAmountRejectsZeroAmount() {
        Obligation obligation = obligation(weekly(TODAY));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.changeAmount(Money.ofCents(0), TODAY, List.of()));

        assertEquals("Obligation amount must be greater than zero", exception.getMessage());
        assertEquals(AMOUNT, obligation.amount());
    }

    @Test
    void changeAmountRejectsArchivedObligation() {
        Obligation obligation = obligation(weekly(TODAY));
        obligation.archive(TODAY, List.of());

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> obligation.changeAmount(Money.ofCents(500), TODAY, List.of()));
    }

    @Test
    void changeAmountRejectsOverdueObligation() {
        Obligation obligation = overdueObligation();

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES, "Obligation has overdue occurrences",
            () -> obligation.changeAmount(Money.ofCents(500), TODAY, List.of()));
        assertEquals(AMOUNT, obligation.amount());
    }

    @Test
    void changeAmountIsAllowedOnceAllPastDatesAreResolved() {
        Obligation obligation = overdueObligation();
        List<OccurrenceResolution> resolutions = List.of(paid(obligation, d(2026, 8, 6)),
            skipped(obligation, d(2026, 8, 13)));

        obligation.changeAmount(Money.ofCents(500), TODAY, resolutions);

        assertEquals(Money.ofCents(500), obligation.amount());
    }

    @Test
    void archivedIsCheckedBeforeOverdue() {
        Obligation obligation = new Obligation(UUID.randomUUID(), userId, "Rent", AMOUNT, account.id(),
            category.id(), weekly(d(2026, 8, 6)), ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> obligation.changeAmount(Money.ofCents(500), TODAY, List.of()));
    }

    // changePaymentSource

    @Test
    void changePaymentSourceUpdatesAccountAndCategory() {
        Obligation obligation = obligation(weekly(TODAY));
        Account otherAccount = new Account(userId, "Savings");
        Category otherCategory = new Category(userId, "Utilities", CategoryType.EXPENSE);

        obligation.changePaymentSource(otherAccount, otherCategory, TODAY, List.of());

        assertEquals(otherAccount.id(), obligation.accountId());
        assertEquals(otherCategory.id(), obligation.categoryId());
    }

    @Test
    void changePaymentSourceRejectsAccountOfAnotherUser() {
        Obligation obligation = obligation(weekly(TODAY));
        UUID otherUser = UUID.randomUUID();
        Account foreignAccount = new Account(otherUser, "Foreign");
        Category foreignCategory = new Category(otherUser, "Foreign", CategoryType.EXPENSE);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.changePaymentSource(foreignAccount, foreignCategory, TODAY, List.of()));

        assertEquals("Account must belong to the obligation user", exception.getMessage());
        assertEquals(account.id(), obligation.accountId());
        assertEquals(category.id(), obligation.categoryId());
    }

    @Test
    void changePaymentSourceRejectsCategoryOfAnotherUser() {
        Obligation obligation = obligation(weekly(TODAY));
        Category foreignCategory = new Category(UUID.randomUUID(), "Foreign", CategoryType.EXPENSE);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.changePaymentSource(account, foreignCategory, TODAY, List.of()));

        assertEquals("Account and category must belong to the same user", exception.getMessage());
    }

    @Test
    void changePaymentSourceRejectsIncomeCategory() {
        Obligation obligation = obligation(weekly(TODAY));
        Category income = new Category(userId, "Salary", CategoryType.INCOME);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.changePaymentSource(account, income, TODAY, List.of()));

        assertEquals("Category type is not valid for an obligation", exception.getMessage());
    }

    @Test
    void changePaymentSourceRejectsInactiveAccountThenInactiveCategory() {
        Obligation obligation = obligation(weekly(TODAY));
        Account inactiveAccount = new Account(userId, "Old");
        inactiveAccount.deactivate();
        Category inactiveCategory = new Category(userId, "Old", CategoryType.EXPENSE);
        inactiveCategory.deactivate();

        assertRule(BusinessRuleCode.ACCOUNT_INACTIVE, "Account must be active",
            () -> obligation.changePaymentSource(inactiveAccount, inactiveCategory, TODAY, List.of()));
        assertRule(BusinessRuleCode.CATEGORY_INACTIVE, "Category must be active",
            () -> obligation.changePaymentSource(account, inactiveCategory, TODAY, List.of()));
        assertEquals(account.id(), obligation.accountId());
        assertEquals(category.id(), obligation.categoryId());
    }

    @Test
    void changePaymentSourceRejectsArchivedObligation() {
        Obligation obligation = obligation(weekly(TODAY));
        obligation.archive(TODAY, List.of());

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> obligation.changePaymentSource(account, category, TODAY, List.of()));
    }

    @Test
    void changePaymentSourceRejectsOverdueObligationAndAllowsItOnceResolved() {
        Obligation obligation = overdueObligation();
        Account otherAccount = new Account(userId, "Savings");

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES, "Obligation has overdue occurrences",
            () -> obligation.changePaymentSource(otherAccount, category, TODAY, List.of()));

        obligation.changePaymentSource(otherAccount, category, TODAY,
            List.of(skipped(obligation, d(2026, 8, 6)), skipped(obligation, d(2026, 8, 13))));

        assertEquals(otherAccount.id(), obligation.accountId());
    }

    @Test
    void changePaymentSourceChecksArchivedBeforeAccountOwnership() {
        Obligation obligation = obligation(weekly(TODAY));
        obligation.archive(TODAY, List.of());
        Account foreignAccount = new Account(UUID.randomUUID(), "Foreign");

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> obligation.changePaymentSource(foreignAccount, category, TODAY, List.of()));
        assertEquals(account.id(), obligation.accountId());
    }

    @Test
    void changePaymentSourceChecksOverdueBeforeAccountOwnership() {
        Obligation obligation = overdueObligation();
        Account foreignAccount = new Account(UUID.randomUUID(), "Foreign");

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES, "Obligation has overdue occurrences",
            () -> obligation.changePaymentSource(foreignAccount, category, TODAY, List.of()));
        assertEquals(account.id(), obligation.accountId());
    }

    // changeRecurrence

    @Test
    void changeRecurrenceRejectsArchivedObligation() {
        Obligation obligation = obligation(weekly(TODAY));
        obligation.archive(TODAY, List.of());

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> obligation.changeRecurrence(monthly(TODAY), TODAY, List.of()));
    }

    @Test
    void changeRecurrenceRejectsOverdueObligation() {
        Obligation obligation = overdueObligation();

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES, "Obligation has overdue occurrences",
            () -> obligation.changeRecurrence(monthly(TODAY), TODAY, List.of()));
        assertEquals(weekly(d(2026, 8, 6)), obligation.recurrence());
    }

    @Test
    void changeRecurrenceChecksOverdueBeforeStartBeforeToday() {
        Obligation obligation = overdueObligation();

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES, "Obligation has overdue occurrences",
            () -> obligation.changeRecurrence(monthly(d(2026, 8, 19)), TODAY, List.of()));
        assertEquals(weekly(d(2026, 8, 6)), obligation.recurrence());
    }

    @Test
    void changeRecurrenceChecksEndBeforeYesterdayBeforeEndConflictWithUpcomingResolution() {
        Obligation obligation = obligation(weekly(d(2026, 8, 1)));
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 8, 1)),
            skipped(obligation, d(2026, 8, 8)), skipped(obligation, d(2026, 8, 15)),
            skipped(obligation, d(2026, 8, 22)));
        Recurrence endBeforeYesterdayAndBeforeUpcoming = new Recurrence(Frequency.WEEKLY, d(2026, 8, 1),
            d(2026, 8, 18));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.changeRecurrence(endBeforeYesterdayAndBeforeUpcoming, TODAY, resolutions));

        assertEquals("End date cannot be before yesterday", exception.getMessage());
        assertEquals(weekly(d(2026, 8, 1)), obligation.recurrence());
    }

    @Test
    void changeRecurrenceAcceptsNewStartDateEqualToToday() {
        Obligation obligation = obligation(monthly(d(2026, 9, 1)));
        Recurrence changed = weekly(TODAY);

        obligation.changeRecurrence(changed, TODAY, List.of());

        assertEquals(changed, obligation.recurrence());
    }

    @Test
    void changeRecurrenceRejectsNewStartDateBeforeToday() {
        Obligation obligation = obligation(monthly(d(2026, 9, 1)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.changeRecurrence(weekly(d(2026, 8, 19)), TODAY, List.of()));

        assertEquals("Start date cannot be before today", exception.getMessage());
        assertEquals(monthly(d(2026, 9, 1)), obligation.recurrence());
    }

    @Test
    void changeRecurrenceRejectsFrequencyChangeKeepingAPastStartDate() {
        Obligation obligation = obligation(weekly(d(2026, 8, 13)));
        List<OccurrenceResolution> resolutions = List.of(paid(obligation, d(2026, 8, 13)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.changeRecurrence(monthly(d(2026, 8, 13)), TODAY, resolutions));

        assertEquals("Start date cannot be before today", exception.getMessage());
    }

    @Test
    void changeRecurrenceAllowsPastResolutionsWhenReAnchoring() {
        Obligation obligation = obligation(weekly(d(2026, 8, 13)));
        List<OccurrenceResolution> resolutions = List.of(paid(obligation, d(2026, 8, 13)));
        Recurrence changed = monthly(TODAY);

        obligation.changeRecurrence(changed, TODAY, resolutions);

        assertEquals(changed, obligation.recurrence());
    }

    @Test
    void changeRecurrenceRejectsNewStartAfterAnUpcomingResolution() {
        Obligation obligation = obligation(monthly(d(2026, 8, 20)));
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 9, 20)));

        assertRule(BusinessRuleCode.OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION,
            "Start date cannot be after a resolved upcoming occurrence",
            () -> obligation.changeRecurrence(weekly(d(2026, 9, 21)), TODAY, resolutions));
        assertEquals(monthly(d(2026, 8, 20)), obligation.recurrence());
    }

    @Test
    void changeRecurrenceAcceptsNewStartEqualToAnUpcomingResolution() {
        Obligation obligation = obligation(monthly(d(2026, 8, 20)));
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 9, 20)));
        Recurrence changed = weekly(d(2026, 9, 20));

        obligation.changeRecurrence(changed, TODAY, resolutions);

        assertEquals(changed, obligation.recurrence());
    }

    @Test
    void changeRecurrenceTreatsAResolutionDueTodayAsUpcoming() {
        Obligation obligation = obligation(monthly(d(2026, 8, 20)));
        List<OccurrenceResolution> resolutions = List.of(paid(obligation, d(2026, 8, 20)));

        assertRule(BusinessRuleCode.OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION,
            "Start date cannot be after a resolved upcoming occurrence",
            () -> obligation.changeRecurrence(weekly(d(2026, 8, 21)), TODAY, resolutions));
    }

    @Test
    void changeRecurrenceAcceptsEndDateEqualToYesterday() {
        Obligation obligation = obligation(weekly(d(2026, 8, 1)));
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 8, 1)),
            skipped(obligation, d(2026, 8, 8)), skipped(obligation, d(2026, 8, 15)));
        Recurrence withEnd = new Recurrence(Frequency.WEEKLY, d(2026, 8, 1), d(2026, 8, 19));

        obligation.changeRecurrence(withEnd, TODAY, resolutions);

        assertEquals(withEnd, obligation.recurrence());
    }

    @Test
    void changeRecurrenceRejectsEndDateBeforeYesterday() {
        Obligation obligation = obligation(weekly(d(2026, 8, 1)));
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 8, 1)),
            skipped(obligation, d(2026, 8, 8)), skipped(obligation, d(2026, 8, 15)));
        Recurrence withEnd = new Recurrence(Frequency.WEEKLY, d(2026, 8, 1), d(2026, 8, 18));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.changeRecurrence(withEnd, TODAY, resolutions));

        assertEquals("End date cannot be before yesterday", exception.getMessage());
        assertEquals(weekly(d(2026, 8, 1)), obligation.recurrence());
    }

    @Test
    void changeRecurrenceRejectsEndDateBeforeAnUpcomingResolution() {
        Obligation obligation = obligation(monthly(d(2026, 8, 20)));
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 9, 20)));
        Recurrence withEnd = new Recurrence(Frequency.MONTHLY, d(2026, 8, 20), d(2026, 9, 19));

        assertRule(BusinessRuleCode.OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION,
            "End date cannot be before a resolved upcoming occurrence",
            () -> obligation.changeRecurrence(withEnd, TODAY, resolutions));
        assertEquals(monthly(d(2026, 8, 20)), obligation.recurrence());
    }

    @Test
    void changeRecurrenceAcceptsEndDateEqualToAnUpcomingResolution() {
        Obligation obligation = obligation(monthly(d(2026, 8, 20)));
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 9, 20)));
        Recurrence withEnd = new Recurrence(Frequency.MONTHLY, d(2026, 8, 20), d(2026, 9, 20));

        obligation.changeRecurrence(withEnd, TODAY, resolutions);

        assertEquals(withEnd, obligation.recurrence());
    }

    @Test
    void changeRecurrenceChecksStartBeforeTodayBeforeEndConflict() {
        Obligation obligation = obligation(monthly(d(2026, 8, 20)));
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 9, 20)));
        Recurrence startBeforeTodayAndEndConflict = new Recurrence(Frequency.WEEKLY, d(2026, 8, 19), d(2026, 9, 1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.changeRecurrence(startBeforeTodayAndEndConflict, TODAY, resolutions));

        assertEquals("Start date cannot be before today", exception.getMessage());
    }

    @Test
    void changeRecurrenceAllowsRemovingTheEndDate() {
        Obligation obligation = obligation(new Recurrence(Frequency.MONTHLY, d(2026, 8, 20), d(2026, 12, 20)));

        obligation.changeRecurrence(monthly(d(2026, 8, 20)), TODAY, List.of());

        assertEquals(Optional.empty(), obligation.recurrence().endDate());
    }

    @Test
    void changeRecurrenceChangingOnlyTheEndDoesNotApplyTheStartRule() {
        Obligation obligation = obligation(weekly(d(2026, 8, 13)));
        List<OccurrenceResolution> resolutions = List.of(paid(obligation, d(2026, 8, 13)));
        Recurrence changed = new Recurrence(Frequency.WEEKLY, d(2026, 8, 13), d(2026, 9, 30));

        obligation.changeRecurrence(changed, TODAY, resolutions);

        assertEquals(changed, obligation.recurrence());
    }

    @Test
    void changeRecurrenceRejectsResolutionOfAnotherObligation() {
        Obligation obligation = obligation(weekly(TODAY));
        Obligation other = obligation(weekly(TODAY));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.changeRecurrence(monthly(TODAY), TODAY, List.of(skipped(other, TODAY))));

        assertEquals("Resolution does not belong to the obligation", exception.getMessage());
    }

    // archive

    @Test
    void archiveMarksTheObligationArchived() {
        Obligation obligation = obligation(weekly(TODAY));

        obligation.archive(TODAY, List.of());

        assertEquals(ObligationStatus.ARCHIVED, obligation.status());
    }

    @Test
    void archivingTwiceIsRejected() {
        Obligation obligation = obligation(weekly(TODAY));
        obligation.archive(TODAY, List.of());

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived",
            () -> obligation.archive(TODAY, List.of()));
    }

    @Test
    void archiveRejectsOverdueObligation() {
        Obligation obligation = overdueObligation();

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES, "Obligation has overdue occurrences",
            () -> obligation.archive(TODAY, List.of()));
        assertEquals(ObligationStatus.ACTIVE, obligation.status());
    }

    @Test
    void archiveIsAllowedOnceAllPastDatesAreResolved() {
        Obligation obligation = overdueObligation();

        obligation.archive(TODAY, List.of(paid(obligation, d(2026, 8, 6)), skipped(obligation, d(2026, 8, 13))));

        assertEquals(ObligationStatus.ARCHIVED, obligation.status());
    }

    // overdue

    @Test
    void overdueCountCoversStartDateThroughYesterday() {
        Obligation obligation = overdueObligation();

        assertEquals(2, obligation.overdueCount(TODAY, List.of()));
    }

    @Test
    void overdueCountDoesNotIncludeToday() {
        Obligation obligation = obligation(weekly(TODAY));

        assertEquals(0, obligation.overdueCount(TODAY, List.of()));
        assertEquals(1, obligation.overdueCount(TODAY.plusDays(1), List.of()));
    }

    @Test
    void overdueCountIsZeroForFutureStart() {
        Obligation obligation = obligation(weekly(d(2026, 9, 1)));

        assertEquals(0, obligation.overdueCount(TODAY, List.of()));
    }

    @Test
    void overdueCountRespectsTheEndDate() {
        Obligation obligation = obligation(new Recurrence(Frequency.WEEKLY, d(2026, 7, 1), d(2026, 7, 15)));

        assertEquals(3, obligation.overdueCount(TODAY, List.of()));
    }

    @Test
    void overdueCountSubtractsResolvedDates() {
        Obligation obligation = overdueObligation();

        assertEquals(1, obligation.overdueCount(TODAY, List.of(paid(obligation, d(2026, 8, 6)))));
        assertEquals(0, obligation.overdueCount(TODAY,
            List.of(paid(obligation, d(2026, 8, 6)), skipped(obligation, d(2026, 8, 13)))));
    }

    @Test
    void overdueCountIgnoresUpcomingResolutions() {
        Obligation obligation = overdueObligation();

        assertEquals(2, obligation.overdueCount(TODAY, List.of(skipped(obligation, d(2026, 8, 20)))));
    }

    @Test
    void overdueCountDoesNotSubtractResolutionsBeforeTheStartDate() {
        Obligation obligation = overdueObligation();
        OccurrenceResolution beforeStart = paid(obligation, d(2026, 7, 30));

        assertEquals(2, obligation.overdueCount(TODAY, List.of(beforeStart)));
    }

    @Test
    void overdueCountDoesNotSubtractResolutionsOffTheCurrentCalendar() {
        Obligation obligation = overdueObligation();
        OccurrenceResolution offCalendar = paid(obligation, d(2026, 8, 7));

        assertEquals(2, obligation.overdueCount(TODAY, List.of(offCalendar)));
    }

    @Test
    void reconstitutedOffCalendarPaidDateDoesNotReduceOverdue() {
        Obligation obligation = obligation(weekly(TODAY));
        OccurrenceResolution paidAhead = paid(obligation, d(2026, 8, 25));
        LocalDate later = d(2026, 9, 1);

        assertEquals(2, obligation.overdueCount(later, List.of(paidAhead)));
        assertEquals(1, obligation.overdueCount(later, List.of(paidAhead, skipped(obligation, TODAY))));
    }

    @Test
    void reAnchoringLeavingAnUpcomingResolutionOffTheNewCalendarIsRejected() {
        Obligation obligation = obligation(monthly(d(2026, 8, 25)));
        List<OccurrenceResolution> resolutions = List.of(paid(obligation, d(2026, 8, 25)));

        assertRule(BusinessRuleCode.OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION,
            "Upcoming resolution is not on the new calendar",
            () -> obligation.changeRecurrence(weekly(d(2026, 8, 21)), TODAY, resolutions));
        assertEquals(monthly(d(2026, 8, 25)), obligation.recurrence());
    }

    @Test
    void reAnchoringIsAcceptedWhenAllUpcomingResolutionsAreOnTheNewCalendar() {
        Obligation obligation = obligation(monthly(d(2026, 8, 27)));
        List<OccurrenceResolution> resolutions = List.of(paid(obligation, d(2026, 8, 27)));
        Recurrence changed = weekly(TODAY);

        obligation.changeRecurrence(changed, TODAY, resolutions);

        assertEquals(changed, obligation.recurrence());
    }

    @Test
    void reAnchoringIgnoresPastResolutionsOffTheNewCalendar() {
        Obligation obligation = obligation(weekly(d(2026, 8, 13)));
        List<OccurrenceResolution> resolutions = List.of(paid(obligation, d(2026, 8, 13)));

        obligation.changeRecurrence(monthly(d(2026, 8, 25)), TODAY, resolutions);

        assertEquals(monthly(d(2026, 8, 25)), obligation.recurrence());
    }

    @Test
    void changingOnlyTheEndNeverChecksUpcomingResolutionsAgainstTheCalendar() {
        Obligation obligation = obligation(weekly(TODAY));
        List<OccurrenceResolution> resolutions = List.of(paid(obligation, d(2026, 8, 25)));
        Recurrence changed = new Recurrence(Frequency.WEEKLY, TODAY, d(2026, 9, 30));

        obligation.changeRecurrence(changed, TODAY, resolutions);

        assertEquals(changed, obligation.recurrence());
    }

    @Test
    void startAfterAnUpcomingResolutionReportsTheStartMessageEvenIfItIsOffTheNewCalendar() {
        Obligation obligation = obligation(monthly(d(2026, 8, 20)));
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 9, 20)));

        assertRule(BusinessRuleCode.OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION,
            "Start date cannot be after a resolved upcoming occurrence",
            () -> obligation.changeRecurrence(weekly(d(2026, 9, 21)), TODAY, resolutions));
    }

    @Test
    void endBeforeAnUpcomingResolutionReportsTheEndMessageEvenIfItIsOffTheNewCalendar() {
        Obligation obligation = obligation(monthly(d(2026, 8, 20)));
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 9, 20)));
        Recurrence changed = new Recurrence(Frequency.WEEKLY, d(2026, 8, 21), d(2026, 9, 19));

        assertRule(BusinessRuleCode.OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION,
            "End date cannot be before a resolved upcoming occurrence",
            () -> obligation.changeRecurrence(changed, TODAY, resolutions));
    }

    private Obligation endedAndResolvedObligation() {
        return obligation(new Recurrence(Frequency.WEEKLY, d(2026, 7, 1), d(2026, 7, 15)));
    }

    private static List<OccurrenceResolution> resolveAll(Obligation obligation, LocalDate... dates) {
        return Arrays.stream(dates).map(date -> skipped(obligation, date)).toList();
    }

    @Test
    void removingTheEndWouldCreateOverdueOccurrences() {
        Obligation obligation = endedAndResolvedObligation();
        List<OccurrenceResolution> resolutions = resolveAll(obligation, d(2026, 7, 1), d(2026, 7, 8), d(2026, 7, 15));

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES, "Change would create overdue occurrences",
            () -> obligation.changeRecurrence(weekly(d(2026, 7, 1)), TODAY, resolutions));
        assertEquals(new Recurrence(Frequency.WEEKLY, d(2026, 7, 1), d(2026, 7, 15)), obligation.recurrence());
    }

    @Test
    void extendingTheEndToYesterdayWouldCreateOverdueOccurrences() {
        Obligation obligation = endedAndResolvedObligation();
        List<OccurrenceResolution> resolutions = resolveAll(obligation, d(2026, 7, 1), d(2026, 7, 8), d(2026, 7, 15));
        Recurrence extended = new Recurrence(Frequency.WEEKLY, d(2026, 7, 1), d(2026, 8, 19));

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES, "Change would create overdue occurrences",
            () -> obligation.changeRecurrence(extended, TODAY, resolutions));
        assertEquals(new Recurrence(Frequency.WEEKLY, d(2026, 7, 1), d(2026, 7, 15)), obligation.recurrence());
    }

    @Test
    void extendingTheEndPastTodayWouldCreateOverdueOccurrences() {
        Obligation obligation = endedAndResolvedObligation();
        List<OccurrenceResolution> resolutions = resolveAll(obligation, d(2026, 7, 1), d(2026, 7, 8), d(2026, 7, 15));
        Recurrence extended = new Recurrence(Frequency.WEEKLY, d(2026, 7, 1), d(2026, 9, 30));

        assertRule(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES, "Change would create overdue occurrences",
            () -> obligation.changeRecurrence(extended, TODAY, resolutions));
        assertEquals(new Recurrence(Frequency.WEEKLY, d(2026, 7, 1), d(2026, 7, 15)), obligation.recurrence());
    }

    @Test
    void extendingAnEndAtOrAfterYesterdayWithoutNewPastDatesIsAccepted() {
        Obligation obligation = obligation(new Recurrence(Frequency.WEEKLY, TODAY, d(2026, 9, 3)));
        Recurrence extended = new Recurrence(Frequency.WEEKLY, TODAY, d(2026, 9, 30));

        obligation.changeRecurrence(extended, TODAY, List.of());

        assertEquals(extended, obligation.recurrence());
    }

    @Test
    void extendingTheEndToYesterdayWithAllPastDatesResolvedIsAccepted() {
        Obligation obligation = obligation(new Recurrence(Frequency.WEEKLY, d(2026, 8, 1), d(2026, 8, 15)));
        List<OccurrenceResolution> resolutions = resolveAll(obligation, d(2026, 8, 1), d(2026, 8, 8), d(2026, 8, 15));
        Recurrence extended = new Recurrence(Frequency.WEEKLY, d(2026, 8, 1), d(2026, 8, 19));

        obligation.changeRecurrence(extended, TODAY, resolutions);

        assertEquals(extended, obligation.recurrence());
    }

    @Test
    void overdueCountRejectsResolutionOfAnotherObligation() {
        Obligation obligation = overdueObligation();
        Obligation other = overdueObligation();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.overdueCount(TODAY, List.of(skipped(other, d(2026, 8, 6)))));

        assertEquals("Resolution does not belong to the obligation", exception.getMessage());
    }

    @Test
    void unresolvedDatesBetweenExcludesResolvedDates() {
        Obligation obligation = obligation(weekly(d(2026, 8, 6)));
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 8, 20)));

        assertEquals(List.of(d(2026, 8, 13), d(2026, 8, 27)),
            obligation.unresolvedDatesBetween(d(2026, 8, 7), d(2026, 8, 30), resolutions));
    }

    // overdueDates

    @Test
    void overdueDatesListsScheduledDatesFromStartThroughYesterdayAscending() {
        Obligation obligation = overdueObligation();

        assertEquals(List.of(d(2026, 8, 6), d(2026, 8, 13)), obligation.overdueDates(TODAY, List.of()));
    }

    @Test
    void overdueDatesExcludesResolvedDatesAndToday() {
        Obligation obligation = overdueObligation();
        List<OccurrenceResolution> resolutions = List.of(skipped(obligation, d(2026, 8, 6)),
            paid(obligation, TODAY));

        assertEquals(List.of(d(2026, 8, 13)), obligation.overdueDates(TODAY, resolutions));
    }

    @Test
    void overdueDatesIsEmptyWhenStartDateIsTodayOrLater() {
        assertEquals(List.of(), obligation(weekly(TODAY)).overdueDates(TODAY, List.of()));
        assertEquals(List.of(), obligation(weekly(d(2026, 9, 1))).overdueDates(TODAY, List.of()));
    }

    @Test
    void overdueDatesIsLimitedByAPastEndDate() {
        Obligation obligation = obligation(new Recurrence(Frequency.WEEKLY, d(2026, 7, 1), d(2026, 7, 15)));

        assertEquals(List.of(d(2026, 7, 1), d(2026, 7, 8), d(2026, 7, 15)),
            obligation.overdueDates(TODAY, List.of()));
    }

    @Test
    void overdueDatesRejectsNullToday() {
        Obligation obligation = overdueObligation();

        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> obligation.overdueDates(null, List.of()));

        assertEquals("Today cannot be null", exception.getMessage());
    }

    @Test
    void overdueDatesRejectsNullResolutions() {
        Obligation obligation = overdueObligation();

        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> obligation.overdueDates(TODAY, null));

        assertEquals("Resolutions cannot be null", exception.getMessage());
    }

    @Test
    void overdueDatesRejectsResolutionOfAnotherObligation() {
        Obligation obligation = overdueObligation();
        Obligation other = overdueObligation();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> obligation.overdueDates(TODAY, List.of(skipped(other, d(2026, 8, 6)))));

        assertEquals("Resolution does not belong to the obligation", exception.getMessage());
    }

    @Test
    void overdueDatesIgnoresOffCalendarResolutionsAndKeepsOnCalendarOverdueDates() {
        Obligation obligation = overdueObligation();
        OccurrenceResolution offCalendar = paid(obligation, d(2026, 8, 7));

        assertEquals(List.of(d(2026, 8, 6), d(2026, 8, 13)),
            obligation.overdueDates(TODAY, List.of(offCalendar)));
    }

    @Test
    void overdueDatesSizeMatchesOverdueCount() {
        Obligation weekly = overdueObligation();
        Obligation ended = obligation(new Recurrence(Frequency.WEEKLY, d(2026, 7, 1), d(2026, 7, 15)));
        Obligation monthly = obligation(monthly(d(2026, 5, 31)));
        Obligation future = obligation(weekly(d(2026, 9, 1)));
        List<OccurrenceResolution> weeklyResolutions = List.of(paid(weekly, d(2026, 8, 7)),
            skipped(weekly, d(2026, 8, 6)));

        assertEquals(weekly.overdueCount(TODAY, weeklyResolutions), weekly.overdueDates(TODAY, weeklyResolutions).size());
        assertEquals(weekly.overdueCount(TODAY, List.of()), weekly.overdueDates(TODAY, List.of()).size());
        assertEquals(ended.overdueCount(TODAY, List.of()), ended.overdueDates(TODAY, List.of()).size());
        assertEquals(monthly.overdueCount(TODAY, List.of()), monthly.overdueDates(TODAY, List.of()).size());
        assertEquals(future.overdueCount(TODAY, List.of()), future.overdueDates(TODAY, List.of()).size());
    }

    // ensureActive

    @Test
    void ensureActiveAcceptsActiveObligation() {
        Obligation active = obligation(weekly(TODAY));

        assertDoesNotThrow(active::ensureActive);
    }

    @Test
    void ensureActiveRejectsArchivedObligation() {
        Obligation archived = new Obligation(UUID.randomUUID(), userId, "Rent", AMOUNT, account.id(), category.id(),
            weekly(TODAY), ObligationStatus.ARCHIVED);

        assertRule(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived", archived::ensureActive);
    }

    // identity

    @Test
    void equalityIsByIdentifier() {
        UUID id = UUID.randomUUID();
        Obligation first = new Obligation(id, userId, "Rent", AMOUNT, account.id(), category.id(), weekly(TODAY),
            ObligationStatus.ACTIVE);
        Obligation same = new Obligation(id, userId, "Other", Money.ofCents(1), account.id(), category.id(),
            monthly(TODAY), ObligationStatus.ARCHIVED);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, obligation(weekly(TODAY)));
    }
}
