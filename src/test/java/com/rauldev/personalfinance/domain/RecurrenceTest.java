package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RecurrenceTest {

    private static LocalDate d(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    @Test
    void rejectsNullFrequency() {
        assertThrows(NullPointerException.class, () -> new Recurrence(null, d(2026, 1, 1), null));
    }

    @Test
    void rejectsNullStartDate() {
        assertThrows(NullPointerException.class, () -> new Recurrence(Frequency.WEEKLY, null, null));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Recurrence(Frequency.WEEKLY, d(2026, 1, 2), d(2026, 1, 1)));

        assertEquals("End date cannot be before start date", exception.getMessage());
    }

    @Test
    void acceptsEndDateEqualToStartDate() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 1), d(2026, 1, 1));

        assertEquals(Optional.of(d(2026, 1, 1)), recurrence.endDate());
    }

    @Test
    void acceptsNullEndDate() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 1), null);

        assertEquals(Optional.empty(), recurrence.endDate());
        assertEquals(Frequency.WEEKLY, recurrence.frequency());
        assertEquals(d(2026, 1, 1), recurrence.startDate());
    }

    @Test
    void hasValueEquality() {
        Recurrence first = new Recurrence(Frequency.MONTHLY, d(2026, 1, 31), null);
        Recurrence same = new Recurrence(Frequency.MONTHLY, d(2026, 1, 31), null);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertEquals(new Recurrence(Frequency.MONTHLY, d(2026, 1, 31), d(2026, 6, 30)),
            new Recurrence(Frequency.MONTHLY, d(2026, 1, 31), d(2026, 6, 30)));
        assertNotEquals(first, new Recurrence(Frequency.MONTHLY, d(2026, 1, 31), d(2026, 6, 30)));
        assertNotEquals(first, new Recurrence(Frequency.WEEKLY, d(2026, 1, 31), null));
        assertNotEquals(first, new Recurrence(Frequency.MONTHLY, d(2026, 1, 30), null));
        assertNotEquals(first, "recurrence");
    }

    @Test
    void yearlyLeapDayFallsBackToFebruary28InNonLeapYears() {
        Recurrence recurrence = new Recurrence(Frequency.YEARLY, d(2024, 2, 29), null);

        assertEquals(List.of(d(2024, 2, 29), d(2025, 2, 28), d(2026, 2, 28), d(2027, 2, 28), d(2028, 2, 29)),
            recurrence.datesBetween(d(2024, 2, 29), d(2028, 2, 29)));
        assertTrue(recurrence.isScheduled(d(2025, 2, 28)));
        assertFalse(recurrence.isScheduled(d(2025, 3, 1)));
        assertTrue(recurrence.isScheduled(d(2028, 2, 29)));
        assertFalse(recurrence.isScheduled(d(2028, 2, 28)));
    }

    @Test
    void monthlyAnchoredOnJanuary31IncludesFebruary29InLeapYear() {
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, d(2024, 1, 31), null);

        assertEquals(List.of(d(2024, 1, 31), d(2024, 2, 29), d(2024, 3, 31)),
            recurrence.datesBetween(d(2024, 1, 1), d(2024, 3, 31)));
    }

    @Test
    void monthlyClampsToMonthEndWithoutCarryOver() {
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, d(2026, 1, 31), null);

        assertEquals(List.of(d(2026, 1, 31), d(2026, 2, 28), d(2026, 3, 31), d(2026, 4, 30), d(2026, 5, 31)),
            recurrence.datesBetween(d(2026, 1, 1), d(2026, 5, 31)));
        assertFalse(recurrence.isScheduled(d(2026, 3, 28)));
        assertTrue(recurrence.isScheduled(d(2026, 2, 28)));
    }

    @Test
    void endDateIsInclusive() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), d(2026, 1, 19));

        assertTrue(recurrence.isScheduled(d(2026, 1, 19)));
        assertFalse(recurrence.isScheduled(d(2026, 1, 26)));
        assertFalse(recurrence.isScheduled(d(2026, 1, 20)));
        assertEquals(3, recurrence.countBetween(d(2026, 1, 1), d(2026, 12, 31)));
        assertEquals(List.of(d(2026, 1, 5), d(2026, 1, 12), d(2026, 1, 19)),
            recurrence.datesBetween(d(2026, 1, 1), d(2026, 12, 31)));
    }

    @Test
    void endDateOnOffCalendarDayExcludesTheDaysAfterTheLastDate() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), d(2026, 1, 18));

        assertEquals(List.of(d(2026, 1, 5), d(2026, 1, 12)), recurrence.datesBetween(d(2026, 1, 1), d(2026, 2, 1)));
    }

    @Test
    void datesBeforeStartAreNotScheduled() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), null);

        assertFalse(recurrence.isScheduled(d(2025, 12, 29)));
        assertEquals(List.of(d(2026, 1, 5)), recurrence.datesBetween(d(2025, 1, 1), d(2026, 1, 5)));
        assertEquals(1, recurrence.countBetween(d(2025, 1, 1), d(2026, 1, 5)));
    }

    @Test
    void onceOnlySchedulesTheStartDate() {
        Recurrence recurrence = new Recurrence(Frequency.ONCE, d(2026, 1, 31), null);

        assertTrue(recurrence.isScheduled(d(2026, 1, 31)));
        assertFalse(recurrence.isScheduled(d(2026, 2, 7)));
        assertFalse(recurrence.isScheduled(d(2026, 2, 28)));
        assertEquals(1, recurrence.countBetween(d(2026, 1, 1), d(2030, 1, 1)));
        assertEquals(0, recurrence.countBetween(d(2026, 2, 1), d(2030, 1, 1)));
        assertEquals(List.of(d(2026, 1, 31)), recurrence.datesBetween(d(2026, 1, 1), d(2030, 1, 1)));
        assertEquals(List.of(), recurrence.datesBetween(d(2026, 2, 1), d(2030, 1, 1)));
    }

    @Test
    void futureStartHasNothingBeforeStart() {
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, d(2026, 6, 10), null);

        assertEquals(List.of(), recurrence.datesBetween(d(2026, 1, 1), d(2026, 6, 9)));
        assertEquals(0, recurrence.countBetween(d(2026, 1, 1), d(2026, 6, 9)));
        assertEquals(Optional.empty(), recurrence.firstDateBetweenExcluding(d(2026, 1, 1), d(2026, 6, 9), Set.of()));
    }

    @Test
    void weeklyStepsExactlySevenDaysAcrossMonthAndYearBoundaries() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2025, 12, 22), null);

        assertEquals(List.of(d(2025, 12, 22), d(2025, 12, 29), d(2026, 1, 5)),
            recurrence.datesBetween(d(2025, 12, 1), d(2026, 1, 10)));
        assertEquals(List.of(d(2026, 1, 26), d(2026, 2, 2), d(2026, 2, 9)),
            new Recurrence(Frequency.WEEKLY, d(2026, 1, 26), null).datesBetween(d(2026, 1, 1), d(2026, 2, 10)));
    }

    @Test
    void biweeklyStepsExactlyFourteenDays() {
        Recurrence recurrence = new Recurrence(Frequency.BIWEEKLY, d(2026, 1, 26), null);

        assertEquals(List.of(d(2026, 1, 26), d(2026, 2, 9), d(2026, 2, 23)),
            recurrence.datesBetween(d(2026, 1, 1), d(2026, 2, 28)));
        assertFalse(recurrence.isScheduled(d(2026, 2, 2)));
        assertTrue(recurrence.isScheduled(d(2026, 2, 9)));
    }

    @Test
    void weeklyTenYearRangeCountsExactly522() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2016, 1, 4), null);

        assertEquals(522, recurrence.countBetween(d(2016, 1, 4), d(2026, 1, 3)));
        assertEquals(522, recurrence.datesBetween(d(2016, 1, 4), d(2026, 1, 3)).size());
    }

    @Test
    void monthlyHundredYearRangeCountsMatchDates() {
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, d(2000, 1, 31), null);

        assertEquals(1200, recurrence.countBetween(d(2000, 1, 31), d(2099, 12, 31)));
        assertEquals(1200, recurrence.datesBetween(d(2000, 1, 31), d(2099, 12, 31)).size());
    }

    @Test
    void yearlyHundredYearRangeCountsMatchDates() {
        Recurrence recurrence = new Recurrence(Frequency.YEARLY, d(2000, 2, 29), null);

        assertEquals(100, recurrence.countBetween(d(2000, 2, 29), d(2099, 12, 31)));
        assertEquals(100, recurrence.datesBetween(d(2000, 2, 29), d(2099, 12, 31)).size());
        assertEquals(99, recurrence.countBetween(d(2000, 2, 29), d(2099, 2, 27)));
    }

    @Test
    void countBetweenIsZeroWhenFromIsAfterTo() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), null);

        assertEquals(0, recurrence.countBetween(d(2026, 2, 1), d(2026, 1, 1)));
        assertEquals(List.of(), recurrence.datesBetween(d(2026, 2, 1), d(2026, 1, 1)));
    }

    @Test
    void countBetweenRoundsUnalignedBoundsInward() {
        Recurrence weekly = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), null);
        Recurrence monthly = new Recurrence(Frequency.MONTHLY, d(2026, 1, 31), null);

        assertEquals(2, weekly.countBetween(d(2026, 1, 6), d(2026, 1, 19)));
        assertEquals(1, weekly.countBetween(d(2026, 1, 6), d(2026, 1, 18)));
        assertEquals(0, weekly.countBetween(d(2026, 1, 6), d(2026, 1, 11)));
        assertEquals(0, monthly.countBetween(d(2026, 2, 1), d(2026, 2, 27)));
        assertEquals(1, monthly.countBetween(d(2026, 2, 1), d(2026, 2, 28)));
        assertEquals(1, monthly.countBetween(d(2026, 2, 28), d(2026, 2, 28)));
    }

    private static List<LocalDate> oracle(Frequency frequency, LocalDate anchor, LocalDate end, LocalDate from,
                                          LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        for (long n = 0; n < 200; n++) {
            LocalDate date = switch (frequency) {
                case ONCE -> anchor;
                case WEEKLY -> anchor.plusDays(7 * n);
                case BIWEEKLY -> anchor.plusDays(14 * n);
                case MONTHLY -> anchor.plusMonths(n);
                case YEARLY -> anchor.plusYears(n);
            };
            if (frequency == Frequency.ONCE && n > 0) {
                break;
            }
            if (end != null && date.isAfter(end)) {
                break;
            }
            if (!date.isBefore(from) && !date.isAfter(to)) {
                dates.add(date);
            }
        }
        return dates;
    }

    @Test
    void matchesABruteForceOracleForEveryFrequencyAndAnchor() {
        LocalDate[] anchors = {d(2024, 1, 28), d(2024, 1, 29), d(2024, 1, 30), d(2024, 1, 31), d(2024, 2, 29),
            d(2025, 1, 31)};
        LocalDate[] froms = {d(2023, 12, 31), d(2024, 1, 29), d(2024, 2, 28), d(2024, 3, 1), d(2025, 2, 27)};
        LocalDate[] tos = {d(2024, 1, 27), d(2024, 2, 29), d(2024, 3, 30), d(2025, 3, 1), d(2026, 12, 31)};
        LocalDate[] ends = {null, d(2025, 2, 28)};
        for (Frequency frequency : Frequency.values()) {
            for (LocalDate anchor : anchors) {
                for (LocalDate end : ends) {
                    Recurrence recurrence = new Recurrence(frequency, anchor, end);
                    for (LocalDate from : froms) {
                        for (LocalDate to : tos) {
                            List<LocalDate> expected = oracle(frequency, anchor, end, from, to);
                            String context = frequency + " " + anchor + " end=" + end + " " + from + ".." + to;

                            assertEquals(expected, recurrence.datesBetween(from, to), context);
                            assertEquals(expected.size(), recurrence.countBetween(from, to), context);
                        }
                    }
                    LocalDate probe = anchor.minusDays(3);
                    for (int i = 0; i < 800; i++) {
                        LocalDate date = probe.plusDays(i);
                        boolean expected = oracle(frequency, anchor, end, date, date).contains(date);

                        assertEquals(expected, recurrence.isScheduled(date), frequency + " " + anchor + " " + date);
                    }
                }
            }
        }
    }

    @Test
    void monthlyAnchoredOnJanuary31IsNotScheduledOnFebruary28OfALeapYear() {
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, d(2024, 1, 31), null);

        assertFalse(recurrence.isScheduled(d(2024, 2, 28)));
        assertTrue(recurrence.isScheduled(d(2024, 2, 29)));
    }

    @Test
    void unresolvedIgnoresResolvedDatesOutOfRangeOrOffCalendar() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), null);
        Set<LocalDate> resolved = Set.of(d(2026, 1, 5), d(2026, 1, 6), d(2025, 1, 1), d(2026, 3, 2));

        assertEquals(List.of(d(2026, 1, 12), d(2026, 1, 19)),
            recurrence.unresolvedDatesBetween(d(2026, 1, 12), d(2026, 1, 25), resolved));
        assertEquals(2, recurrence.countUnresolvedBetween(d(2026, 1, 12), d(2026, 1, 25), resolved));
        assertEquals(List.of(d(2026, 1, 12), d(2026, 1, 19)),
            recurrence.unresolvedDatesBetween(d(2026, 1, 5), d(2026, 1, 25), resolved));
        assertEquals(2, recurrence.countUnresolvedBetween(d(2026, 1, 5), d(2026, 1, 25), resolved));
    }

    @Test
    void unresolvedIsEmptyForEmptyOrInvertedRange() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), null);

        assertEquals(0, recurrence.countUnresolvedBetween(d(2026, 1, 6), d(2026, 1, 11), Set.of()));
        assertEquals(List.of(), recurrence.unresolvedDatesBetween(d(2026, 1, 6), d(2026, 1, 11), Set.of()));
        assertEquals(0, recurrence.countUnresolvedBetween(d(2026, 2, 1), d(2026, 1, 1), Set.of(d(2026, 1, 5))));
        assertEquals(List.of(), recurrence.unresolvedDatesBetween(d(2026, 2, 1), d(2026, 1, 1), Set.of()));
    }

    @Test
    void unresolvedCountEqualsAscendingListSize() {
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, d(2026, 1, 31), null);
        Set<LocalDate> resolved = Set.of(d(2026, 2, 28), d(2026, 4, 30));

        List<LocalDate> dates = recurrence.unresolvedDatesBetween(d(2026, 1, 1), d(2026, 6, 30), resolved);

        assertEquals(List.of(d(2026, 1, 31), d(2026, 3, 31), d(2026, 5, 31), d(2026, 6, 30)), dates);
        assertEquals(dates.size(), recurrence.countUnresolvedBetween(d(2026, 1, 1), d(2026, 6, 30), resolved));
    }

    @Test
    void unresolvedRejectsNullResolvedDates() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), null);

        NullPointerException count = assertThrows(NullPointerException.class,
            () -> recurrence.countUnresolvedBetween(d(2026, 1, 5), d(2026, 2, 1), null));
        NullPointerException list = assertThrows(NullPointerException.class,
            () -> recurrence.unresolvedDatesBetween(d(2026, 1, 5), d(2026, 2, 1), null));

        assertEquals("Resolved dates cannot be null", count.getMessage());
        assertEquals("Resolved dates cannot be null", list.getMessage());
    }

    @Test
    void firstDateBetweenExcludingSkipsExcludedDates() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), null);

        assertEquals(Optional.of(d(2026, 1, 19)), recurrence.firstDateBetweenExcluding(d(2026, 1, 5), d(2026, 2, 1),
            Set.of(d(2026, 1, 5), d(2026, 1, 12))));
        assertEquals(Optional.of(d(2026, 1, 5)),
            recurrence.firstDateBetweenExcluding(d(2026, 1, 5), d(2026, 2, 1), Set.of()));
    }

    @Test
    void firstDateBetweenExcludingIsEmptyWhenAllAreExcluded() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), null);

        assertEquals(Optional.empty(), recurrence.firstDateBetweenExcluding(d(2026, 1, 5), d(2026, 1, 12),
            Set.of(d(2026, 1, 5), d(2026, 1, 12))));
    }

    @Test
    void firstDateBetweenExcludingIsEmptyForEmptyRange() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), null);

        assertEquals(Optional.empty(), recurrence.firstDateBetweenExcluding(d(2026, 2, 1), d(2026, 1, 1), Set.of()));
        assertEquals(Optional.empty(), recurrence.firstDateBetweenExcluding(d(2026, 1, 6), d(2026, 1, 11), Set.of()));
    }

    @Test
    void firstDateBetweenExcludingIgnoresExcludedDatesOffTheCalendar() {
        Recurrence recurrence = new Recurrence(Frequency.WEEKLY, d(2026, 1, 5), null);

        assertEquals(Optional.of(d(2026, 1, 5)), recurrence.firstDateBetweenExcluding(d(2026, 1, 5), d(2026, 2, 1),
            Set.of(d(2026, 1, 6), d(2026, 1, 4))));
    }
}
