// Calendar arithmetic and Spanish texts of `DatePickerSheet`. Dates are ISO calendar strings
// ("2026-10-15") computed in UTC, so no device time zone shifts a day. The design system imports
// nothing from lib, so the picker keeps its own small month and weekday tables.

const MS_PER_DAY = 24 * 60 * 60 * 1000;
const MONTHS_SHORT = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const MONTHS_LONG = [
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'octubre',
  'noviembre',
  'diciembre',
];
const WEEKDAYS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];

/** Column headers of the month grid: the week runs Monday to Sunday. */
export const WEEKDAY_INITIALS = ['L', 'M', 'M', 'J', 'V', 'S', 'D'] as const;

/** A month of the calendar: year and month (1–12). */
export interface CalendarMonth {
  year: number;
  month: number;
}

/** Year, month (1–12), and day of an ISO date. */
function parts(iso: string): { year: number; month: number; day: number } {
  const [year = 0, month = 1, day = 1] = iso.split('-').map(Number);
  return { year, month, day };
}

/** ISO date of a year, month (1–12), and day. */
function isoOf(year: number, month: number, day: number): string {
  return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

/** Day of the week of an ISO date: 0 is Sunday. */
function weekday(iso: string): number {
  const { year, month, day } = parts(iso);
  return new Date(Date.UTC(year, month - 1, day)).getUTCDay();
}

/** Number of days in a month. */
function daysInMonth({ year, month }: CalendarMonth): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

/** The month an ISO date belongs to. */
export function monthOf(iso: string): CalendarMonth {
  const { year, month } = parts(iso);
  return { year, month };
}

/** The month `offset` months after `month` (before it when negative). */
export function shiftMonth({ year, month }: CalendarMonth, offset: number): CalendarMonth {
  const index = year * 12 + (month - 1) + offset;
  return { year: Math.floor(index / 12), month: (index % 12) + 1 };
}

/** First and last day of a month, as ISO dates. */
export function monthBounds(month: CalendarMonth): { first: string; last: string } {
  return { first: isoOf(month.year, month.month, 1), last: isoOf(month.year, month.month, daysInMonth(month)) };
}

/**
 * The month's days as weeks of seven, Monday first; null fills the days of the neighbouring months.
 */
export function monthWeeks(month: CalendarMonth): (string | null)[][] {
  const lead = (weekday(isoOf(month.year, month.month, 1)) + 6) % 7;
  const cells: (string | null)[] = Array.from({ length: lead }, () => null);
  for (let day = 1; day <= daysInMonth(month); day += 1) {
    cells.push(isoOf(month.year, month.month, day));
  }
  while (cells.length % 7 !== 0) {
    cells.push(null);
  }
  const weeks: (string | null)[][] = [];
  for (let start = 0; start < cells.length; start += 7) {
    weeks.push(cells.slice(start, start + 7));
  }
  return weeks;
}

/** Day of the month of an ISO date, as shown in its cell. */
export function dayNumber(iso: string): string {
  return String(parts(iso).day);
}

/** Month title: "Octubre 2026". */
export function monthTitle({ year, month }: CalendarMonth): string {
  const name = MONTHS_LONG[month - 1] ?? '';
  return `${name.charAt(0).toUpperCase()}${name.slice(1)} ${year}`;
}

/** Short month name for the navigation buttons: "sep". */
export function monthShortName({ month }: CalendarMonth): string {
  return MONTHS_SHORT[month - 1] ?? '';
}

/** Full month name for the navigation buttons' accessible names: "septiembre". */
export function monthLongName({ month }: CalendarMonth): string {
  return MONTHS_LONG[month - 1] ?? '';
}

/** Short date: "15 oct", or "1 jun 2027" when its year is not today's. */
export function shortDate(today: string, iso: string): string {
  const { year, month, day } = parts(iso);
  const text = `${day} ${MONTHS_SHORT[month - 1] ?? ''}`;
  return year === parts(today).year ? text : `${text} ${year}`;
}

/** Weekday and short date, lowercase: "jueves 15 oct". */
export function weekdayDate(today: string, iso: string): string {
  return `${WEEKDAYS[weekday(iso)] ?? ''} ${shortDate(today, iso)}`;
}

/** Days from `fromIso` to `toIso` (negative when `toIso` is earlier). */
export function compareDates(fromIso: string, toIso: string): number {
  const from = parts(fromIso);
  const to = parts(toIso);
  return (Date.UTC(to.year, to.month - 1, to.day) - Date.UTC(from.year, from.month - 1, from.day)) / MS_PER_DAY;
}
