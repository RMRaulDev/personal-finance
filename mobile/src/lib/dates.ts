// API dates are ISO-8601 calendar dates ("2026-10-07") without a time zone. They stay strings:
// `new Date('YYYY-MM-DD')` parses them as UTC midnight and can shift the day on the device.

const MS_PER_DAY = 24 * 60 * 60 * 1000;

// Own tables instead of Intl, so the text is identical on every device and matches the design
// ("7 oct", without a trailing dot).
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

/** Year, month (1–12), and day of an ISO calendar date. */
interface DateParts {
  year: number;
  month: number;
  day: number;
}

/** Splits an ISO calendar date into its numeric parts. Throws on any other format. */
function parseIsoDate(iso: string): DateParts {
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

/** Short day and month in Spanish: `"7 oct"`. */
export function formatDayMonth(iso: string): string {
  const { month, day } = parseIsoDate(iso);
  return `${day} ${MONTHS_SHORT[month - 1]}`;
}

/** Weekday, day, and month in Spanish, capitalized: `"Martes 7 de octubre"`. */
export function formatLongDate(iso: string): string {
  const { year, month, day } = parseIsoDate(iso);
  const weekday = WEEKDAYS[new Date(Date.UTC(year, month - 1, day)).getUTCDay()];
  const text = `${weekday} ${day} de ${MONTHS_LONG[month - 1]}`;
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/**
 * Days after today that an upcoming due date is written as "en N días · 15 oct" instead of only
 * the date, as the "Dashboard Finanzas" design does. Copy logic, not a style.
 */
const RELATIVE_DUE_DAYS = 6;

/**
 * Day of a past operation relative to `today`: `"Hoy"`, `"Ayer"`, or the short date (`"7 oct"`).
 * `today` is the server's (the dashboard's `horizon.from`), never the device's.
 */
export function formatRelativeDay(today: string, date: string): string {
  const days = daysBetween(date, today);
  if (days === 0) {
    return 'Hoy';
  }
  if (days === 1) {
    return 'Ayer';
  }
  return formatDayMonth(date);
}

/**
 * Upcoming due date relative to `today`: `"hoy"`, `"mañana"`, `"en N días · 15 oct"` up to six days
 * ahead, or the short date after that. `today` is the server's, never the device's.
 */
export function formatDueDate(today: string, dueDate: string): string {
  const days = daysBetween(today, dueDate);
  if (days === 0) {
    return 'hoy';
  }
  if (days === 1) {
    return 'mañana';
  }
  if (days <= RELATIVE_DUE_DAYS) {
    return `en ${days} días · ${formatDayMonth(dueDate)}`;
  }
  return formatDayMonth(dueDate);
}
