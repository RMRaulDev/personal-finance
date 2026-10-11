// API dates are ISO-8601 calendar dates ("2026-10-07") without a time zone. They stay strings:
// `new Date('YYYY-MM-DD')` parses them as UTC midnight and can shift the day on the device.

const MS_PER_DAY = 24 * 60 * 60 * 1000;

/** Year, month (1–12), and day of an ISO calendar date. */
export interface DateParts {
  year: number;
  month: number;
  day: number;
}

/** Splits an ISO calendar date into its numeric parts. Throws on any other format. */
export function parseIsoDate(iso: string): DateParts {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
  if (!match) {
    throw new Error(`Invalid ISO date: ${iso}`);
  }
  return { year: Number(match[1]), month: Number(match[2]), day: Number(match[3]) };
}

/** Builds the ISO calendar date of a year, month (1–12), and day. */
export function toIsoDate({ year, month, day }: DateParts): string {
  return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

/** Days since the epoch for a calendar date, computed in UTC so no local offset applies. */
function epochDay(iso: string): number {
  const { year, month, day } = parseIsoDate(iso);
  return Date.UTC(year, month - 1, day) / MS_PER_DAY;
}

/** Whole days from `fromIso` to `toIso` (negative when `toIso` is earlier). */
export function daysBetween(fromIso: string, toIso: string): number {
  return epochDay(toIso) - epochDay(fromIso);
}

/** The calendar date `days` days after `iso` (before it when negative), in UTC so no offset applies. */
export function addDays(iso: string, days: number): string {
  const date = new Date((epochDay(iso) + days) * MS_PER_DAY);
  return toIsoDate({ year: date.getUTCFullYear(), month: date.getUTCMonth() + 1, day: date.getUTCDate() });
}

/** Day of the week of a calendar date: 0 is Sunday, 6 is Saturday. */
export function weekdayOf(iso: string): number {
  return new Date(epochDay(iso) * MS_PER_DAY).getUTCDay();
}

/**
 * The first date strictly after `afterIso` whose day of the month is `day`. Only for days that
 * every month has (1–28), such as the 1st and the 15th of the form's quick dates.
 */
export function nextDayOfMonth(afterIso: string, day: number): string {
  const { year, month } = parseIsoDate(afterIso);
  const sameMonth = toIsoDate({ year, month, day });
  if (daysBetween(afterIso, sameMonth) > 0) {
    return sameMonth;
  }
  return month === 12 ? toIsoDate({ year: year + 1, month: 1, day }) : toIsoDate({ year, month: month + 1, day });
}
