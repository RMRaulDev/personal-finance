// API dates stay ISO strings (see parse.ts); these helpers only turn them into Spanish text.

import { parseIsoDate } from './parse';

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
