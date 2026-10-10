import { formatDayMonth } from './format';
import { daysBetween } from './parse';

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
