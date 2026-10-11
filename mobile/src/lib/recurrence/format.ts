// Spanish text for an obligation's calendar ("Mensual · día 5") and the form's summary sentence.
// Only text: the occurrences themselves are computed by the server, never here.

import {
  daysBetween,
  formatDayMonth,
  formatMonthName,
  formatShortDate,
  formatWeekdayDate,
  formatWeekdayPlural,
  parseIsoDate,
} from '../dates';
import { formatMoney } from '../money';

/**
 * How often an obligation repeats. The same names as the API's `Frequency`; it is declared here
 * because lib imports nothing from api, and TypeScript checks the two stay compatible at each call.
 */
export type RecurrenceFrequency = 'ONCE' | 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'YEARLY';

/**
 * Last day every month has. A monthly schedule anchored after it is paid on the last day of the
 * shorter months, which the summary explains.
 */
const LAST_DAY_OF_EVERY_MONTH = 28;

/**
 * Frequency in words with its anchor, as the "Compromisos v1" design writes it: "Una sola vez ·
 * 20 oct", "Semanal · sábados", "Cada 2 semanas · viernes", "Mensual · día 5", "Anual · 5 nov".
 * `today` is the server's, so a date in another year shows its year.
 */
export function formatFrequency(today: string, frequency: RecurrenceFrequency, startDate: string): string {
  switch (frequency) {
    case 'ONCE':
      return `Una sola vez · ${formatShortDate(today, startDate)}`;
    case 'WEEKLY':
      return `Semanal · ${formatWeekdayPlural(startDate)}`;
    case 'BIWEEKLY':
      return `Cada 2 semanas · ${formatWeekdayPlural(startDate)}`;
    case 'MONTHLY':
      return `Mensual · día ${parseIsoDate(startDate).day}`;
    case 'YEARLY':
      return `Anual · ${formatDayMonth(startDate)}`;
  }
}

/** What the form's summary sentence describes. */
export interface ScheduleSummaryInput {
  /** The server's today. */
  today: string;
  amountCents: number;
  frequency: RecurrenceFrequency;
  startDate: string;
  /** Last payment, included; null when it repeats until archived. Ignored for `ONCE`. */
  endDate: string | null;
  /**
   * Adds the warning that a start date in the past leaves that payment overdue. Only true while
   * creating: an existing obligation's past start was already settled.
   */
  warnPastStart: boolean;
}

/**
 * The form's summary in plain language, e.g. "Pagarás $4,800 cada mes, el día 15, desde el 15 oct.",
 * plus the design's two notes: the last day of short months, and a first payment already overdue.
 */
export function formatScheduleSummary({
  today,
  amountCents,
  frequency,
  startDate,
  endDate,
  warnPastStart,
}: ScheduleSummaryInput): string {
  const amount = formatMoney(amountCents);
  const since = `desde el ${formatShortDate(today, startDate)}`;
  const { day } = parseIsoDate(startDate);
  let text: string;
  switch (frequency) {
    case 'ONCE':
      text = `Pagarás ${amount} una sola vez, el ${formatWeekdayDate(today, startDate).toLowerCase()}`;
      break;
    case 'WEEKLY':
      text = `Pagarás ${amount} cada semana, los ${formatWeekdayPlural(startDate)}, ${since}`;
      break;
    case 'BIWEEKLY':
      text = `Pagarás ${amount} cada 2 semanas, los ${formatWeekdayPlural(startDate)}, ${since}`;
      break;
    case 'MONTHLY':
      text = `Pagarás ${amount} cada mes, el día ${day}, ${since}`;
      break;
    case 'YEARLY':
      text = `Pagarás ${amount} cada año, el ${day} de ${formatMonthName(startDate)}, ${since}`;
      break;
  }
  if (frequency !== 'ONCE' && endDate !== null) {
    text += `, hasta el ${formatShortDate(today, endDate)}`;
  }
  text += '.';
  if (frequency === 'MONTHLY' && day > LAST_DAY_OF_EVERY_MONTH) {
    text += ` Si un mes no tiene día ${day}, se paga el último día.`;
  }
  if (warnPastStart && daysBetween(today, startDate) < 0) {
    text += ` El pago del ${formatShortDate(today, startDate)} quedará como vencido para que lo pagues o lo saltes.`;
  }
  return text;
}
