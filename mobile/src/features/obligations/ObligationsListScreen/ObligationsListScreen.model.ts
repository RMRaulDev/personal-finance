import type { AccountResponse, CategoryResponse, IsoDate, ObligationResponse } from '../../../api';
import { daysBetween, formatDueDate, formatMoney, formatShortDate } from '../../../lib';
import type { ArchivedRowData } from '../components/ArchivedSection';
import type { ObligationRowData } from '../components/ObligationRow';
import { blockedReason, frequencyText, plural } from '../obligations.model';

// Pure mapping from the obligations to the rows of the list screen ("Compromisos").

/** One active obligation as the list shows it, with the id its row opens. */
export interface ActiveRow extends ObligationRowData {
  id: string;
}

/** Next payment relative to `today`: "pendiente desde el 5 sep", "hoy", "mañana", "en 5 días · 15 oct". */
function whenText(today: IsoDate, dueDate: IsoDate): string {
  return daysBetween(today, dueDate) < 0
    ? `pendiente desde el ${formatShortDate(today, dueDate)}`
    : formatDueDate(today, dueDate);
}

/** Sort key of an active row: its next unresolved date; rows with nothing pending go last. */
function compareNextDue(a: ObligationResponse, b: ObligationResponse): number {
  if (a.nextDueDate === b.nextDueDate) {
    return 0;
  }
  if (a.nextDueDate === null) {
    return 1;
  }
  if (b.nextDueDate === null) {
    return -1;
  }
  return a.nextDueDate < b.nextDueDate ? -1 : 1;
}

/**
 * Active obligations ordered by their oldest unresolved occurrence (the oldest overdue, else the next
 * pending, as the server computes `nextDueDate`), so what is overdue comes first; those with nothing
 * pending go last. Ties keep the server's order (by name).
 */
export function activeRows(
  obligations: ObligationResponse[],
  accounts: AccountResponse[] | undefined,
  categories: CategoryResponse[] | undefined,
  today: IsoDate,
): ActiveRow[] {
  return obligations
    .filter((obligation) => obligation.status === 'ACTIVE')
    .sort(compareNextDue)
    .map((obligation) => {
      const overdue = obligation.overdueCount;
      const blocked = blockedReason(obligation, accounts, categories) !== null;
      let badge: ObligationRowData['badge'] = null;
      let status: string;
      if (overdue > 0) {
        badge = { text: `${overdue} ${plural(overdue, 'vencida', 'vencidas')}`, role: 'warning' };
        status = obligation.nextDueDate ? `desde el ${formatShortDate(today, obligation.nextDueDate)}` : '';
      } else {
        status = obligation.nextDueDate
          ? `Próximo pago: ${whenText(today, obligation.nextDueDate)}`
          : 'Sin pagos pendientes';
        if (blocked) {
          badge = { text: 'Pago bloqueado', role: 'info' };
        }
      }
      const amount = formatMoney(obligation.amountCents);
      const frequency = frequencyText(obligation, today);
      return {
        id: obligation.id,
        name: obligation.name,
        amount,
        frequency,
        badge,
        status,
        accessibilityLabel: [obligation.name, amount, frequency, badge?.text, status]
          .filter((part) => part !== undefined && part !== '')
          .join(', '),
      };
    });
}

/** Archived obligations, in the server's order (by name). */
export function archivedRows(obligations: ObligationResponse[], today: IsoDate): ArchivedRowData[] {
  return obligations
    .filter((obligation) => obligation.status === 'ARCHIVED')
    .map((obligation) => {
      const archivedOn = obligation.archivedOn;
      // Obligations archived before the date was recorded have none: "Archivado", without a date.
      const status = archivedOn ? `Archivado el ${formatShortDate(today, archivedOn)}` : 'Archivado';
      return {
        id: obligation.id,
        name: obligation.name,
        amount: formatMoney(obligation.amountCents),
        status,
        accessibilityLabel: `${obligation.name}, ${status.toLowerCase()}`,
      };
    });
}

/** Caption above the active rows: "4 activos · por próximo pago". */
export function activeCaption(count: number): string {
  return `${count} ${plural(count, 'activo', 'activos')} · por próximo pago`;
}

/** Text of the archived toggle: "Archivados (2) ▾", "▴" while expanded. */
export function archivedToggleLabel(count: number, expanded: boolean): string {
  return `Archivados (${count}) ${expanded ? '▴' : '▾'}`;
}

/** Heading of the list's load failure. */
export const LOAD_FAILURE_TITLE = 'No pudimos cargar tus compromisos';
