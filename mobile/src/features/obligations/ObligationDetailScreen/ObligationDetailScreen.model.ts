import type { CategoryResponse, IsoDate, ObligationOccurrencesResponse, ObligationResponse } from '../../../api';
import type { ConfirmDialogProps } from '../../../design-system';
import { daysBetween, formatDayDistance, formatMoney, formatShortDate, formatWeekdayDate } from '../../../lib';
import type { Fact } from '../components/FactList';
import type { ResolvedRowData } from '../components/ResolvedList';
import type { UpcomingRowData } from '../components/UpcomingList';
import { frequencyText, plural } from '../obligations.model';

// Pure mapping from an obligation and its occurrences to what its detail screen shows: the texts of
// each block, the dialogs, and the toasts. The dates and states come from the server; this only
// writes them.

/** The texts of a confirmation dialog, without its handlers. */
export type DialogCopy = Pick<ConfirmDialogProps, 'title' | 'body' | 'note' | 'confirmLabel' | 'showCancel'>;

/** Which dialog is open on the detail. */
export type DetailDialog = 'archive' | 'archiveBlocked' | 'skipAll' | 'alreadyResolved';

/** "Archivado el 20 ago. Ya no cuenta…", without the date when it was not recorded. */
export function archivedText(obligation: ObligationResponse, today: IsoDate): string {
  const when = obligation.archivedOn ? ` el ${formatShortDate(today, obligation.archivedOn)}` : '';
  return `Archivado${when}. Ya no cuenta en Disponible y no se puede editar ni pagar. Su historial se conserva.`;
}

/** Text of the blocked notice: why, and the two ways out. */
export function blockedText(reason: string): string {
  return `${reason} Al pagar, elige otra; o edita el compromiso para que los siguientes pagos ya la tengan.`;
}

/** Title of the overdue block: "2 pagos vencidos". */
export function overdueTitle(count: number): string {
  return `${count} ${plural(count, 'pago vencido', 'pagos vencidos')}`;
}

/** The overdue dates in words: "5 sep y 5 oct", or a range ("del 5 jun al 5 oct") for more than 3. */
function overdueDatesText(today: IsoDate, dates: IsoDate[]): string {
  const first = dates[0];
  const last = dates[dates.length - 1];
  if (dates.length > 3 && first !== undefined && last !== undefined) {
    return `del ${formatShortDate(today, first)} al ${formatShortDate(today, last)}`;
  }
  return dates.map((date) => formatShortDate(today, date)).join(' y ');
}

/** Second line of the overdue block: "5 sep y 5 oct · $550 cada uno". */
export function overdueSubtitle(today: IsoDate, dates: IsoDate[], amountCents: number): string {
  const each = dates.length > 1 ? ' cada uno' : '';
  return `${overdueDatesText(today, dates)} · ${formatMoney(amountCents)}${each}`;
}

/** "Pagar la más antigua · 5 sep", or "Pagar · 5 sep" when only one is overdue. */
export function payOldestLabel(today: IsoDate, dates: IsoDate[]): string {
  const oldest = dates[0] ?? today;
  return dates.length > 1
    ? `Pagar la más antigua · ${formatShortDate(today, oldest)}`
    : `Pagar · ${formatShortDate(today, oldest)}`;
}

/** "Saltar todas las vencidas (2)", or "Saltar la vencida" when only one is overdue. */
export function skipAllLabel(count: number): string {
  return count > 1 ? `Saltar todas las vencidas (${count})` : 'Saltar la vencida';
}

/** Rows of "Por resolver" / "Próximos": the overdue dates, then the next pending ones. */
export function upcomingRows(
  occurrences: ObligationOccurrencesResponse,
  amountCents: number,
  today: IsoDate,
): UpcomingRowData[] {
  const amount = formatMoney(amountCents);
  return [...occurrences.overdue, ...occurrences.upcoming].map((occurrence) => {
    const overdue = occurrence.status === 'OVERDUE';
    const distance = formatDayDistance(today, occurrence.dueDate);
    return {
      dueDate: occurrence.dueDate,
      date: formatWeekdayDate(today, occurrence.dueDate),
      amount,
      overdue,
      when: overdue ? distance : `Pendiente · ${distance}`,
    };
  });
}

/**
 * Rows of "Resueltos", newest due date first as the server sends them. A payment made before its due
 * date (or for a date still ahead) reads "Pagado por adelantado". The expense's category is not in
 * the response, so a payment with another category does not name it.
 */
export function resolvedRows(occurrences: ObligationOccurrencesResponse, today: IsoDate): ResolvedRowData[] {
  return occurrences.resolved.map((occurrence) => {
    const date = formatWeekdayDate(today, occurrence.dueDate);
    const expense = occurrence.expense;
    if (occurrence.status === 'PAID' && expense !== null) {
      const ahead = daysBetween(today, occurrence.dueDate) > 0 || expense.operationDate < occurrence.dueDate;
      const paid = ahead ? 'Pagado por adelantado el' : 'Pagado el';
      return {
        key: occurrence.dueDate,
        date,
        paid: true,
        detail: `${paid} ${formatShortDate(today, expense.operationDate)} · ${formatMoney(expense.amountCents)}`,
        actionLabel: 'Deshacer pago',
        action: { kind: 'undoPayment', operationId: expense.id },
      };
    }
    return {
      key: occurrence.dueDate,
      date,
      paid: false,
      detail: 'Saltado · sin gasto',
      actionLabel: 'Reabrir',
      action: { kind: 'reopen', dueDate: occurrence.dueDate },
    };
  });
}

/** "Detalles": amount, frequency, account, category (marked when inactive), first and last payment. */
export function detailFacts(
  obligation: ObligationResponse,
  categories: CategoryResponse[] | undefined,
  today: IsoDate,
): Fact[] {
  const { recurrence } = obligation;
  const categoryInactive = categories?.some(
    (category) => category.id === obligation.category.id && category.status !== 'ACTIVE',
  );
  const once = recurrence.frequency === 'ONCE';
  const facts: Fact[] = [
    { label: 'Monto', value: formatMoney(obligation.amountCents) },
    { label: 'Frecuencia', value: frequencyText(obligation, today) },
    { label: 'Se paga desde', value: obligation.account.name },
    { label: 'Categoría', value: `${obligation.category.name}${categoryInactive ? ' · inactiva' : ''}` },
    { label: once ? 'Fecha' : 'Primer pago', value: formatShortDate(today, recurrence.startDate) },
  ];
  if (!once) {
    facts.push({
      label: 'Último pago',
      value: recurrence.endDate ? formatShortDate(today, recurrence.endDate) : 'Sin fecha de fin',
    });
  }
  return facts;
}

/** Texts of each dialog of the detail. */
export function dialogCopy(
  dialog: DetailDialog,
  name: string,
  overdueDates: IsoDate[],
  overdueCount: number,
  today: IsoDate,
): DialogCopy {
  switch (dialog) {
    case 'archive':
      return {
        title: `¿Archivar ${name}?`,
        body: 'Dejará de contar en Disponible y ya no podrás editarlo ni pagarlo. Su historial de pagos se conserva.',
        note: 'En esta versión, archivar no se puede deshacer.',
        confirmLabel: 'Archivar',
      };
    case 'archiveBlocked':
      return {
        title: 'Primero resuelve lo vencido',
        body: `${name} tiene ${overdueTitle(overdueCount)}. Págalos o sáltalos y después podrás archivarlo.`,
        confirmLabel: 'Ver vencidos',
        showCancel: false,
      };
    case 'skipAll': {
      const count = overdueDates.length;
      const dates = overdueDates.map((date) => formatShortDate(today, date)).join(', ');
      return {
        title: count > 1 ? `¿Saltar ${count} pagos vencidos?` : '¿Saltar el pago vencido?',
        body: `${name}: ${dates}. No se registra ningún gasto y dejan de contar en Disponible.`,
        note: 'Puedes reabrir cada uno desde «Resueltos».',
        confirmLabel: count > 1 ? `Saltar ${count} pagos` : 'Saltar pago',
      };
    }
    case 'alreadyResolved':
      // The server does not say who resolved it, so the design's "desde otro dispositivo" is left out.
      return {
        title: 'Este pago ya estaba resuelto',
        body: `Actualizamos ${name}; no se hizo ningún cargo nuevo.`,
        confirmLabel: 'Entendido',
        showCancel: false,
      };
  }
}

/** Toast after paying: "Pagaste Internet y teléfono · $1,200". */
export function paidToastText(name: string, amountCents: number): string {
  return `Pagaste ${name} · ${formatMoney(amountCents)}`;
}

/** Toast after skipping one date: "Saltaste Gimnasio del 5 oct.". */
export function skippedToastText(name: string, dueDate: IsoDate, today: IsoDate): string {
  return `Saltaste ${name} del ${formatShortDate(today, dueDate)}.`;
}

/** Toast after skipping every overdue date: "Saltaste 2 pagos de Gimnasio.". */
export function skippedAllToastText(name: string, count: number): string {
  return `Saltaste ${count} ${plural(count, 'pago', 'pagos')} de ${name}.`;
}

/** Toast after undoing a payment: "Deshiciste el pago de Renta.". */
export function undonePaymentToastText(name: string): string {
  return `Deshiciste el pago de ${name}.`;
}

/** Toast after reopening one date: "Reabriste Gimnasio del 5 oct.". */
export function reopenedToastText(name: string, dueDate: IsoDate, today: IsoDate): string {
  return `Reabriste ${name} del ${formatShortDate(today, dueDate)}.`;
}

/** Toast after archiving, shown on the screen it returns to. */
export function archivedToastText(name: string): string {
  return `Archivaste ${name}. Ya no cuenta en Disponible.`;
}
