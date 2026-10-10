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

/** Days since the epoch for a calendar date, computed in UTC so no local offset applies. */
function epochDay(iso: string): number {
  const { year, month, day } = parseIsoDate(iso);
  return Date.UTC(year, month - 1, day) / MS_PER_DAY;
}

/** Whole days from `fromIso` to `toIso` (negative when `toIso` is earlier). */
export function daysBetween(fromIso: string, toIso: string): number {
  return epochDay(toIso) - epochDay(fromIso);
}
