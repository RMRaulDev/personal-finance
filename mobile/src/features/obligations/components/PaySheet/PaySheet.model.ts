import type { IsoDate } from '../../../../api';
import type { ChoiceOption } from '../../../../design-system';
import { addDays, daysBetween, formatMoney, formatShortDate } from '../../../../lib';
import type { Fact } from '../FactList';

// Pure rules of the payment sheet ("Pagar {nombre}"): its texts, the payment date choices, and the
// copy of its errors.

/** What a payment failure shows in the sheet: an optional title and the sentence. */
export interface PayError {
  title?: string;
  text: string;
}

/** Error under the amount when it is empty, $0, or negative. */
export const PAY_AMOUNT_ERROR = 'Escribe un monto mayor a $0.';

/** Error under the categories when none is chosen (the obligation's may be inactive). */
export const PAY_CATEGORY_ERROR = 'Elige una categoría activa.';

/** Error under the accounts when none is chosen (the obligation's may be inactive). Not in the design. */
export const PAY_ACCOUNT_ERROR = 'Elige una cuenta activa.';

/** Help under the payment dates. */
export const PAY_DATE_HELP = 'No puede ser una fecha futura.';

/** Value of the "Otra fecha…" chip, which opens the month sheet; never a date. */
export const OTHER_DATE = 'OTHER';

/** How far back the month sheet lets a payment be dated, in days, as the design's picker allows. */
export const PAY_DATE_MAX_DAYS_BACK = 120;

/** Second line under the title: when the occurrence is due, and that paying ahead registers today. */
export function dueText(today: IsoDate, dueDate: IsoDate): string {
  const days = daysBetween(today, dueDate);
  if (days < 0) {
    return `Venció el ${formatShortDate(today, dueDate)}`;
  }
  if (days === 0) {
    return 'Vence hoy';
  }
  return `Vence el ${formatShortDate(today, dueDate)}. Se registra hoy como pago por adelantado.`;
}

/** The fixed payment dates: today, yesterday, and the due date when it is older than yesterday. */
function fixedDates(today: IsoDate, dueDate: IsoDate): ChoiceOption<string>[] {
  const options: ChoiceOption<string>[] = [
    { value: today, label: 'Hoy' },
    { value: addDays(today, -1), label: 'Ayer' },
  ];
  // A due date of yesterday is already "Ayer"; a future one cannot be a payment date.
  if (daysBetween(today, dueDate) < -1) {
    options.push({ value: dueDate, label: `${formatShortDate(today, dueDate)} (vencimiento)` });
  }
  return options;
}

/**
 * Chips of "Fecha del pago": Hoy, Ayer, the due date when it is older ("5 sep (vencimiento)"), and
 * "Otra fecha…", which shows the picked date instead once one is chosen from the month sheet.
 */
export function dateOptions(today: IsoDate, dueDate: IsoDate, picked: IsoDate): ChoiceOption<string>[] {
  const fixed = fixedDates(today, dueDate);
  const custom = !fixed.some((option) => option.value === picked);
  return [...fixed, { value: OTHER_DATE, label: custom ? formatShortDate(today, picked) : 'Otra fecha…' }];
}

/** The selected chip of "Fecha del pago": the picked date, or "Otra fecha…" for any other date. */
export function dateChoice(today: IsoDate, dueDate: IsoDate, picked: IsoDate): string {
  return fixedDates(today, dueDate).some((option) => option.value === picked) ? picked : OTHER_DATE;
}

/** Prefilled data of the compact view: amount, account, category, and date ("Hoy · 10 oct"). */
export function paymentFacts(params: {
  today: IsoDate;
  amountCents: number;
  accountName: string | undefined;
  categoryName: string | undefined;
  date: IsoDate;
}): Fact[] {
  const { today, amountCents, accountName, categoryName, date } = params;
  return [
    { label: 'Monto', value: formatMoney(amountCents) },
    { label: 'Cuenta', value: accountName ?? '—' },
    { label: 'Categoría', value: categoryName ?? '—' },
    {
      label: 'Fecha',
      value: date === today ? `Hoy · ${formatShortDate(today, today)}` : formatShortDate(today, date),
    },
  ];
}

/** Help under a changed amount: only this payment changes. */
export function changedAmountHelp(baseAmountCents: number): string {
  return `Solo cambia este pago. Los siguientes siguen en ${formatMoney(baseAmountCents)}.`;
}

/** The sheet's error when the account does not have enough balance, with its current balance. */
export function insufficientBalanceError(accountName: string, balanceCents: number, amountCents: number): PayError {
  return {
    title: 'Saldo insuficiente',
    text: `${accountName} tiene ${formatMoney(balanceCents)} y este pago es de ${formatMoney(amountCents)}. Paga un monto menor o pásale dinero a ${accountName} y vuelve a intentarlo.`,
  };
}
