package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregate Root for a payment commitment of the user, single or recurring.
 *
 * The aggregate does not own its {@link OccurrenceResolution}s. Methods that depend on them receive {@code today}
 * and every resolution of this obligation, and derive the overdue occurrences from its current calendar.
 */
public final class Obligation {
    private static final long MAX_START_DATE_DAYS_IN_PAST = 31;

    private final UUID id;
    private final UUID userId;
    private String name;
    private Money amount;
    private UUID accountId;
    private UUID categoryId;
    private Recurrence recurrence;
    private ObligationStatus status;

    public Obligation(UUID id, UUID userId, String name, Money amount, UUID accountId, UUID categoryId,
                      Recurrence recurrence, ObligationStatus status) {
        this.id = Objects.requireNonNull(id, "Obligation id cannot be null");
        this.userId = Objects.requireNonNull(userId, "User id cannot be null");
        this.name = validateName(name);
        this.amount = requirePositive(amount);
        this.accountId = Objects.requireNonNull(accountId, "Account id cannot be null");
        this.categoryId = Objects.requireNonNull(categoryId, "Category id cannot be null");
        this.recurrence = Objects.requireNonNull(recurrence, "Recurrence cannot be null");
        this.status = Objects.requireNonNull(status, "Obligation status cannot be null");
    }

    public static Obligation create(Account account, Category category, String name, Money amount,
                                    Recurrence recurrence, LocalDate today) {
        validatePaymentSource(account, category);
        Objects.requireNonNull(recurrence, "Recurrence cannot be null");
        Objects.requireNonNull(today, "Today cannot be null");
        if (recurrence.startDate().isBefore(today.minusDays(MAX_START_DATE_DAYS_IN_PAST))) {
            throw new IllegalArgumentException("Start date cannot be more than 31 days before today");
        }
        return new Obligation(UUID.randomUUID(), account.userId(), name, amount, account.id(), category.id(),
            recurrence, ObligationStatus.ACTIVE);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String name() {
        return name;
    }

    public Money amount() {
        return amount;
    }

    public UUID accountId() {
        return accountId;
    }

    public UUID categoryId() {
        return categoryId;
    }

    public Recurrence recurrence() {
        return recurrence;
    }

    public ObligationStatus status() {
        return status;
    }

    public void rename(String name) {
        ensureActive();
        this.name = validateName(name);
    }

    public void changeAmount(Money amount, LocalDate today, Collection<OccurrenceResolution> resolutions) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        ensureSchedulable(today, resolutions);
        this.amount = requirePositive(amount);
    }

    /**
     * Points the obligation to a new account and category, validated with the same rules and order as
     * {@link Expense#register}. Callers changing only one of them pass the current other one.
     */
    public void changePaymentSource(Account account, Category category, LocalDate today,
                                    Collection<OccurrenceResolution> resolutions) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        ensureSchedulable(today, resolutions);
        if (!account.userId().equals(userId)) {
            throw new IllegalArgumentException("Account must belong to the obligation user");
        }
        validatePaymentSource(account, category);
        this.accountId = account.id();
        this.categoryId = category.id();
    }

    /**
     * Replaces the calendar. Changing the frequency or the start date re-anchors it: the new start date must be
     * today or later, not after any resolution due today or later, and every resolution due today or later must be
     * on the new calendar. A new end date must be yesterday or later and not before any resolution due today or
     * later. The resulting calendar cannot have overdue occurrences.
     */
    public void changeRecurrence(Recurrence recurrence, LocalDate today,
                                 Collection<OccurrenceResolution> resolutions) {
        Objects.requireNonNull(recurrence, "Recurrence cannot be null");
        ensureSchedulable(today, resolutions);
        Set<LocalDate> resolvedDates = resolvedDates(resolutions);
        List<LocalDate> upcomingResolvedDates = resolvedDates.stream()
            .filter(dueDate -> !dueDate.isBefore(today))
            .toList();

        boolean anchorChanged = recurrence.frequency() != this.recurrence.frequency()
            || !recurrence.startDate().equals(this.recurrence.startDate());
        if (anchorChanged) {
            LocalDate startDate = recurrence.startDate();
            if (startDate.isBefore(today)) {
                throw new IllegalArgumentException("Start date cannot be before today");
            }
            if (upcomingResolvedDates.stream().anyMatch(startDate::isAfter)) {
                throw new BusinessRuleViolationException(
                    BusinessRuleCode.OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION,
                    "Start date cannot be after a resolved upcoming occurrence");
            }
        }

        Optional<LocalDate> endDate = recurrence.endDate();
        if (endDate.isPresent() && !endDate.equals(this.recurrence.endDate())) {
            LocalDate end = endDate.get();
            if (end.isBefore(today.minusDays(1))) {
                throw new IllegalArgumentException("End date cannot be before yesterday");
            }
            if (upcomingResolvedDates.stream().anyMatch(end::isBefore)) {
                throw new BusinessRuleViolationException(
                    BusinessRuleCode.OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION,
                    "End date cannot be before a resolved upcoming occurrence");
            }
        }

        if (anchorChanged && !upcomingResolvedDates.stream().allMatch(recurrence::isScheduled)) {
            throw new BusinessRuleViolationException(
                BusinessRuleCode.OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION,
                "Upcoming resolution is not on the new calendar");
        }
        if (overdueCount(recurrence, today, resolvedDates) > 0) {
            throw new BusinessRuleViolationException(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES,
                "Change would create overdue occurrences");
        }
        this.recurrence = recurrence;
    }

    public void archive(LocalDate today, Collection<OccurrenceResolution> resolutions) {
        ensureSchedulable(today, resolutions);
        status = ObligationStatus.ARCHIVED;
    }

    /**
     * Counts the scheduled dates between the start date and yesterday that have no resolution.
     */
    public long overdueCount(LocalDate today, Collection<OccurrenceResolution> resolutions) {
        Objects.requireNonNull(today, "Today cannot be null");
        return overdueCount(recurrence, today, resolvedDates(resolutions));
    }

    /**
     * Lists the scheduled dates within {@code [from, to]} (both inclusive) that have no resolution.
     */
    public List<LocalDate> unresolvedDatesBetween(LocalDate from, LocalDate to,
                                                  Collection<OccurrenceResolution> resolutions) {
        return recurrence.unresolvedDatesBetween(from, to, resolvedDates(resolutions));
    }

    private static long overdueCount(Recurrence recurrence, LocalDate today, Set<LocalDate> resolvedDates) {
        return recurrence.countUnresolvedBetween(recurrence.startDate(), today.minusDays(1), resolvedDates);
    }

    private void ensureActive() {
        if (status == ObligationStatus.ARCHIVED) {
            throw new BusinessRuleViolationException(BusinessRuleCode.OBLIGATION_ARCHIVED, "Obligation is archived");
        }
    }

    private void ensureSchedulable(LocalDate today, Collection<OccurrenceResolution> resolutions) {
        Objects.requireNonNull(today, "Today cannot be null");
        Objects.requireNonNull(resolutions, "Resolutions cannot be null");
        ensureActive();
        if (overdueCount(today, resolutions) > 0) {
            throw new BusinessRuleViolationException(BusinessRuleCode.OBLIGATION_HAS_OVERDUE_OCCURRENCES,
                "Obligation has overdue occurrences");
        }
    }

    private Set<LocalDate> resolvedDates(Collection<OccurrenceResolution> resolutions) {
        Objects.requireNonNull(resolutions, "Resolutions cannot be null");
        Set<LocalDate> dates = new HashSet<>();
        for (OccurrenceResolution resolution : resolutions) {
            Objects.requireNonNull(resolution, "Resolution cannot be null");
            if (!resolution.obligationId().equals(id)) {
                throw new IllegalArgumentException("Resolution does not belong to the obligation");
            }
            dates.add(resolution.dueDate());
        }
        return dates;
    }

    private static void validatePaymentSource(Account account, Category category) {
        OperationReferences.validate(account, category, CategoryType.EXPENSE,
            "Category type is not valid for an obligation");
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name, "Obligation name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Obligation name cannot be empty");
        }
        return name;
    }

    private static Money requirePositive(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Obligation amount must be greater than zero");
        }
        return amount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Obligation obligation)) {
            return false;
        }
        return id.equals(obligation.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
