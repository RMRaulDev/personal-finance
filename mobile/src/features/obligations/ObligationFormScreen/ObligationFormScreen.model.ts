import {
  isApiError,
  type AccountResponse,
  type CategoryResponse,
  type CreateObligationRequest,
  type Frequency,
  type Horizon,
  type IsoDate,
  type ModifyObligationRequest,
  type ObligationOccurrencesResponse,
  type ObligationResponse,
} from '../../../api';
import type { ChoiceOption } from '../../../design-system';
import {
  addDays,
  amountInputFromCents,
  daysBetween,
  formatMoney,
  formatScheduleSummary,
  formatShortDate,
  formatWeekdayDate,
  nextDayOfMonth,
  parseAmountInput,
} from '../../../lib';
import { activeAccounts, errorMessage } from '../../../shared';
import type { Fact } from '../components/FactList';
import { frequencyText, plural } from '../obligations.model';

// Pure rules of the obligation form ("Nuevo compromiso" / "Editar {nombre}"): its choices, texts,
// client-side validation (which only improves the experience; the server decides), the request it
// sends, and how server errors map to the form.

/** How many days back a new obligation's first payment may be, as the server allows. */
const MAX_START_DAYS_BACK = 31;

/** Most characters of a name, as the design's field allows. */
export const NAME_MAX_LENGTH = 40;

/** What the form edits. Dates are ISO; the amount is the field's text. */
export interface FormValues {
  name: string;
  amount: string;
  frequency: Frequency;
  startDate: IsoDate | null;
  /** "Hasta una fecha" chosen; ignored for `ONCE`. */
  hasEnd: boolean;
  endDate: IsoDate | null;
  accountId: string | null;
  categoryId: string | null;
}

/** Errors shown next to each field; undefined when the field is fine. */
export interface FormErrors {
  name?: string;
  amount?: string;
  start?: string;
  end?: string;
  category?: string;
  account?: string;
}

/** A failure shown above the save button: a title and what to do. */
export interface FormNotice {
  title: string;
  text: string;
}

/** Choices of "Frecuencia", in the design's order. */
export const FREQUENCY_OPTIONS: ChoiceOption<Frequency>[] = [
  { value: 'ONCE', label: 'Una sola vez' },
  { value: 'WEEKLY', label: 'Semanal' },
  { value: 'BIWEEKLY', label: 'Cada 2 semanas' },
  { value: 'MONTHLY', label: 'Mensual' },
  { value: 'YEARLY', label: 'Anual' },
];

/** Choices of "Fin". */
export const END_OPTIONS: ChoiceOption<'none' | 'date'>[] = [
  { value: 'none', label: 'Sin fecha de fin' },
  { value: 'date', label: 'Hasta una fecha' },
];

/** Copy of the form's errors, from the design. */
export const FORM_ERRORS = {
  nameEmpty: 'Escribe un nombre. Por ejemplo, «Renta» o «Internet».',
  amount: 'Escribe un monto mayor a $0.',
  startEmpty: 'Elige la fecha del primer pago.',
  startPast: 'Al cambiar la frecuencia o las fechas, el nuevo primer pago debe ser hoy o después.',
  endEmpty: 'Elige la fecha del último pago.',
  category: 'Elige una categoría.',
  // Not in the design: an account always comes preselected unless the user has none active.
  account: 'Elige una cuenta activa.',
  network: 'Revisa tu conexión y vuelve a intentarlo. Lo que escribiste sigue aquí.',
} as const;

/** "Ya tienes un compromiso llamado «Renta»…": names are unique ignoring case, archived included. */
export function nameDuplicateError(name: string): string {
  return `Ya tienes un compromiso llamado «${name}» (puede estar entre los archivados). Usa un nombre distinto.`;
}

/** The first payment is too old: the earliest allowed date is 31 days back. */
export function startTooOldError(today: IsoDate): string {
  return `El primer pago puede ser de hasta 31 días atrás (desde el ${formatShortDate(today, addDays(today, -MAX_START_DAYS_BACK))}). Si empezó antes, elige la fecha del próximo pago.`;
}

/** The last payment is before the first one. */
export function endBeforeStartError(today: IsoDate, startDate: IsoDate): string {
  return `El último pago no puede ser antes del primero (${formatShortDate(today, startDate)}).`;
}

/**
 * The server rejected the new calendar because a payment made ahead would fall outside it. It does
 * not say which one, so the earliest advance payment the form knows of is named; without one, the
 * sentence is general.
 */
export function calendarConflictNotice(today: IsoDate, paidAheadDate: IsoDate | null): FormNotice {
  const text =
    paidAheadDate !== null
      ? `Ya pagaste por adelantado el ${formatShortDate(today, paidAheadDate)} y ese pago quedaría fuera del nuevo calendario. Elige un calendario que incluya esa fecha o deshaz ese pago desde el detalle.`
      : 'Ya pagaste por adelantado un pago que quedaría fuera del nuevo calendario. Elige un calendario que lo incluya o deshaz ese pago desde el detalle.';
  return { title: 'No se pudo cambiar el calendario', text };
}

/** The earliest payment on or after today, among the resolutions the form loaded. */
export function earliestPaidAhead(
  occurrences: ObligationOccurrencesResponse | undefined,
  today: IsoDate,
): IsoDate | null {
  const dates = (occurrences?.resolved ?? [])
    .filter((occurrence) => occurrence.status === 'PAID' && daysBetween(today, occurrence.dueDate) >= 0)
    .map((occurrence) => occurrence.dueDate)
    .sort();
  return dates[0] ?? null;
}

/**
 * Values the form starts with: empty and monthly for a new obligation (with the first active
 * account), the current ones to edit. An account or category that is no longer active cannot be
 * kept, so editing starts without it and `validate` asks for another: the account falls back to the
 * only active one when there is exactly one (the form shows it as a fact, not a choice), else to
 * none; the category always to none. Saving then sends the replacement as a change.
 */
export function initialValues(
  obligation: ObligationResponse | undefined,
  accounts: AccountResponse[],
  categories: CategoryResponse[],
): FormValues {
  const active = activeAccounts(accounts);
  if (obligation === undefined) {
    return {
      name: '',
      amount: '',
      frequency: 'MONTHLY',
      startDate: null,
      hasEnd: false,
      endDate: null,
      accountId: active[0]?.id ?? null,
      categoryId: null,
    };
  }
  const { recurrence } = obligation;
  const accountActive = active.some((account) => account.id === obligation.account.id);
  const categoryActive = categories.some(
    (category) => category.id === obligation.category.id && category.status === 'ACTIVE',
  );
  const onlyActive = active.length === 1 ? (active[0]?.id ?? null) : null;
  return {
    name: obligation.name,
    amount: amountInputFromCents(obligation.amountCents),
    frequency: recurrence.frequency,
    startDate: recurrence.startDate,
    hasEnd: recurrence.endDate !== null,
    endDate: recurrence.endDate,
    accountId: accountActive ? obligation.account.id : onlyActive,
    categoryId: categoryActive ? obligation.category.id : null,
  };
}

/**
 * Quick dates of "Primer pago": Hoy, Mañana, and the next 15th and the next 1st after tomorrow, in
 * date order, so the common case needs no calendar.
 */
export function quickStartDates(today: IsoDate): ChoiceOption<string>[] {
  const tomorrow = addDays(today, 1);
  const monthDays = [nextDayOfMonth(tomorrow, 15), nextDayOfMonth(tomorrow, 1)].sort();
  return [
    { value: today, label: 'Hoy' },
    { value: tomorrow, label: 'Mañana' },
    ...monthDays.map((date) => ({ value: date, label: formatShortDate(today, date) })),
  ];
}

/** Earliest first payment the month sheet offers: 31 days back for a new one, today when editing. */
export function minStartDate(today: IsoDate, editing: boolean): IsoDate {
  return editing ? today : addDays(today, -MAX_START_DAYS_BACK);
}

/** Label of the start date: "Fecha del pago" for a single payment, "Primer pago" otherwise. */
export function startLabel(frequency: Frequency): string {
  return frequency === 'ONCE' ? 'Fecha del pago' : 'Primer pago';
}

/** Help under the start date. */
export function startHelp(editing: boolean): string {
  return editing
    ? 'Si cambias la frecuencia o las fechas, el nuevo primer pago debe ser hoy o después.'
    : 'Puede ser de hasta 31 días atrás.';
}

/** Help under "Fin". */
export function endHelp(hasEnd: boolean): string {
  return hasEnd ? 'El último pago se incluye.' : 'Se repite hasta que lo archives.';
}

/** The field's text of a chosen date: "Jueves 15 oct". */
export function dateText(today: IsoDate, date: IsoDate | null): string | undefined {
  return date === null ? undefined : formatWeekdayDate(today, date);
}

/** The end date the form would save: null without an end or for a single payment. */
function savedEndDate(values: FormValues): IsoDate | null {
  return values.frequency !== 'ONCE' && values.hasEnd ? values.endDate : null;
}

/** Whether the calendar (frequency, first payment, or end) differs from the obligation's. */
export function calendarChanged(values: FormValues, obligation: ObligationResponse): boolean {
  const { recurrence } = obligation;
  return (
    values.frequency !== recurrence.frequency ||
    values.startDate !== recurrence.startDate ||
    savedEndDate(values) !== recurrence.endDate
  );
}

/** The amount in cents when the field holds an amount above $0, else null. */
export function amountCents(values: FormValues): number | null {
  const parsed = parseAmountInput(values.amount);
  return parsed.kind === 'ok' && parsed.cents > 0 ? parsed.cents : null;
}

/** Names compare as the server does: Unicode-normalized and without case ("Renta" = "RENTA"). */
function sameName(a: string, b: string): boolean {
  return a.normalize('NFC').toLowerCase() === b.normalize('NFC').toLowerCase();
}

/** What `validate` needs besides the values. */
export interface ValidationContext {
  today: IsoDate;
  /** The obligation being edited; undefined while creating. */
  editing: ObligationResponse | undefined;
  /** Every obligation of the user, archived included, to catch a repeated name before saving. */
  obligations: ObligationResponse[] | undefined;
  /** Editing an obligation with overdue payments: only the name can change. */
  locked: boolean;
}

/** Checks the form before saving, as the design does, so the server's 400s are only a fallback. */
export function validate(values: FormValues, context: ValidationContext): FormErrors {
  const { today, editing, obligations, locked } = context;
  const errors: FormErrors = {};
  const name = values.name.trim();
  if (name === '') {
    errors.name = FORM_ERRORS.nameEmpty;
  } else if (obligations?.some((other) => other.id !== editing?.id && sameName(other.name, name))) {
    errors.name = nameDuplicateError(name);
  }
  if (locked) {
    return errors;
  }
  if (amountCents(values) === null) {
    errors.amount = FORM_ERRORS.amount;
  }
  const changed = editing !== undefined && calendarChanged(values, editing);
  if (values.startDate === null) {
    errors.start = FORM_ERRORS.startEmpty;
  } else if (editing === undefined && daysBetween(minStartDate(today, false), values.startDate) < 0) {
    errors.start = startTooOldError(today);
  } else if (changed && daysBetween(today, values.startDate) < 0) {
    errors.start = FORM_ERRORS.startPast;
  }
  if (values.frequency !== 'ONCE' && values.hasEnd) {
    if (values.endDate === null) {
      errors.end = FORM_ERRORS.endEmpty;
    } else if (values.startDate !== null && daysBetween(values.startDate, values.endDate) < 0) {
      errors.end = endBeforeStartError(today, values.startDate);
    }
  }
  if (values.accountId === null) {
    errors.account = FORM_ERRORS.account;
  }
  if (values.categoryId === null) {
    errors.category = FORM_ERRORS.category;
  }
  return errors;
}

/** True when `validate` found nothing. */
export function isValid(errors: FormErrors): boolean {
  return Object.values(errors).every((error) => error === undefined);
}

/** Body of creating the obligation; only called after `validate` passed. */
export function createRequest(values: FormValues): CreateObligationRequest | null {
  const cents = amountCents(values);
  if (cents === null || values.startDate === null || values.accountId === null || values.categoryId === null) {
    return null;
  }
  return {
    name: values.name.trim(),
    amountCents: cents,
    accountId: values.accountId,
    categoryId: values.categoryId,
    recurrence: { frequency: values.frequency, startDate: values.startDate, endDate: savedEndDate(values) },
  };
}

/**
 * Body of changing the obligation: only what changed (an empty object when nothing did). While
 * `locked` only the name can change. A changed calendar is sent whole, as the API replaces it. An
 * inactive account or category replaced in the form differs from the obligation's, so it is sent.
 */
export function modifyRequest(
  values: FormValues,
  obligation: ObligationResponse,
  locked: boolean,
): ModifyObligationRequest {
  const changes: ModifyObligationRequest = {};
  const name = values.name.trim();
  if (name !== obligation.name) {
    changes.name = name;
  }
  if (locked) {
    return changes;
  }
  const cents = amountCents(values);
  if (cents !== null && cents !== obligation.amountCents) {
    changes.amountCents = cents;
  }
  if (values.accountId !== null && values.accountId !== obligation.account.id) {
    changes.accountId = values.accountId;
  }
  if (values.categoryId !== null && values.categoryId !== obligation.category.id) {
    changes.categoryId = values.categoryId;
  }
  if (values.startDate !== null && calendarChanged(values, obligation)) {
    changes.recurrence = {
      frequency: values.frequency,
      startDate: values.startDate,
      endDate: savedEndDate(values),
    };
  }
  return changes;
}

/** The summary in plain language, or '' until there is an amount and a first payment. */
export function summaryText(values: FormValues, today: IsoDate, creating: boolean): string {
  const cents = amountCents(values);
  if (cents === null || values.startDate === null) {
    return '';
  }
  return formatScheduleSummary({
    today,
    amountCents: cents,
    frequency: values.frequency,
    startDate: values.startDate,
    endDate: savedEndDate(values),
    warnPastStart: creating,
  });
}

/** The notice of a form locked by overdue payments. */
export function lockText(name: string, overdueCount: number): string {
  return `${name} tiene ${overdueCount} ${plural(overdueCount, 'pago vencido', 'pagos vencidos')}. Por ahora solo puedes cambiar el nombre. Para cambiar el monto, las fechas o archivarlo, primero paga o salta lo vencido.`;
}

/** The read-only facts of a locked form. */
export function lockedFacts(obligation: ObligationResponse, today: IsoDate): Fact[] {
  const { recurrence } = obligation;
  return [
    { label: 'Monto', value: formatMoney(obligation.amountCents) },
    { label: 'Frecuencia', value: frequencyText(obligation, today) },
    { label: 'Se paga desde', value: obligation.account.name },
    { label: 'Categoría', value: obligation.category.name },
    {
      label: 'Último pago',
      value: recurrence.endDate ? formatShortDate(today, recurrence.endDate) : 'Sin fecha de fin',
    },
  ];
}

/**
 * Toast after creating, shown on the screen it returns to: it already counts in "Disponible", or its
 * first payment falls after the dashboard's horizon.
 */
export function createdToastText(name: string, startDate: IsoDate, horizon: Horizon): string {
  if (daysBetween(horizon.to, startDate) > 0) {
    const days = daysBetween(horizon.from, horizon.to) + 1;
    return `Agregaste ${name}. Su primer pago es el ${formatShortDate(horizon.from, startDate)}, después de los próximos ${days} días.`;
  }
  return `Agregaste ${name}. Ya cuenta en Disponible.`;
}

/** Toast after saving changes. */
export const SAVED_TOAST_TEXT = 'Cambios guardados.';

/** How a failed save shows in the form: errors next to fields, or a notice above the button. */
export interface SaveFailure {
  errors?: FormErrors;
  notice?: FormNotice;
}

/** What `saveFailure` needs to place a server error. */
export interface SaveFailureContext {
  values: FormValues;
  today: IsoDate;
  editing: ObligationResponse | undefined;
  occurrences: ObligationOccurrencesResponse | undefined;
}

/**
 * Maps a failed save to the form. A repeated name goes to the name; a calendar conflict names the
 * advance payment in the way; a 400 is one of the date rules the form checks first (it only gets
 * here if the form's "today" was behind the server's), so it goes to the date it concerns; no
 * connection keeps what was typed. Anything else uses the shared error copy.
 */
export function saveFailure(error: unknown, { values, today, editing, occurrences }: SaveFailureContext): SaveFailure {
  if (isApiError(error) && error.kind === 'network') {
    return { notice: { title: 'No pudimos guardar', text: FORM_ERRORS.network } };
  }
  if (isApiError(error) && error.status === 409 && error.code === 'OBLIGATION_NAME_ALREADY_EXISTS') {
    return { errors: { name: nameDuplicateError(values.name.trim()) } };
  }
  if (isApiError(error) && error.status === 409 && error.code === 'OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION') {
    return { notice: calendarConflictNotice(today, earliestPaidAhead(occurrences, today)) };
  }
  if (isApiError(error) && error.status === 400 && values.startDate !== null) {
    const end = savedEndDate(values);
    if (end !== null && daysBetween(values.startDate, end) < 0) {
      return { errors: { end: endBeforeStartError(today, values.startDate) } };
    }
    if (editing === undefined) {
      return { errors: { start: startTooOldError(today) } };
    }
    if (calendarChanged(values, editing)) {
      return { errors: { start: FORM_ERRORS.startPast } };
    }
  }
  return { notice: { title: 'No pudimos guardar', text: errorMessage(error) } };
}
