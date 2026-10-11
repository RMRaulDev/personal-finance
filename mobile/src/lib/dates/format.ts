// API dates stay ISO strings (see parse.ts); these helpers only turn them into Spanish text.

import { parseIsoDate, weekdayOf } from './parse';

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
// "los sábados", "los lunes": the plural only changes the weekend days.
const WEEKDAYS_PLURAL = ['domingos', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábados'];

/** Capitalizes the first letter of a sentence fragment. */
function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** Short day and month in Spanish: `"7 oct"`. */
export function formatDayMonth(iso: string): string {
  const { month, day } = parseIsoDate(iso);
  return `${day} ${MONTHS_SHORT[month - 1]}`;
}

/**
 * Short date relative to `today` (the server's): `"15 oct"`, or `"1 jun 2027"` when the year is not
 * today's, as the "Compromisos v1" design writes every date.
 */
export function formatShortDate(today: string, iso: string): string {
  const sameYear = parseIsoDate(today).year === parseIsoDate(iso).year;
  return sameYear ? formatDayMonth(iso) : `${formatDayMonth(iso)} ${parseIsoDate(iso).year}`;
}

/** Weekday and short date, capitalized: `"Jueves 15 oct"` (with the year when it is not today's). */
export function formatWeekdayDate(today: string, iso: string): string {
  return capitalize(`${WEEKDAYS[weekdayOf(iso)]} ${formatShortDate(today, iso)}`);
}

/** Weekday, day, and month in Spanish, capitalized: `"Martes 7 de octubre"`. */
export function formatLongDate(iso: string): string {
  const { month, day } = parseIsoDate(iso);
  return capitalize(`${WEEKDAYS[weekdayOf(iso)]} ${day} de ${MONTHS_LONG[month - 1]}`);
}

/** Plural weekday of a date, for repeating schedules: `"sábados"` ("los sábados"). */
export function formatWeekdayPlural(iso: string): string {
  return WEEKDAYS_PLURAL[weekdayOf(iso)] ?? '';
}

/** Full month name of a date, lowercase: `"noviembre"`. */
export function formatMonthName(iso: string): string {
  return MONTHS_LONG[parseIsoDate(iso).month - 1] ?? '';
}
