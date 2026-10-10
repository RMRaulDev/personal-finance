import type {
  AccountResponse,
  AccountShortfallItem,
  CategoryResponse,
  DashboardResponse,
  IsoDate,
  ObligationResponse,
  PaymentBlockedItem,
  RecentActivityItem,
} from '../../api';
import type { ChoiceOption } from '../../design-system';
import { daysBetween, formatDayMonth, formatDueDate, formatMoney } from '../../lib';

import { recentLimit } from './designSpec';

// Pure mapping from the API response to what the screen renders. Keeping it out of the components
// makes every rule of the design (texts, order, limits) readable in one place.

/** One row of "Próximos compromisos". */
export interface CommitmentRow {
  /** Unique within the list: obligation id plus due date. */
  key: string;
  obligationId: string;
  /** Occurrence to pay. For an overdue obligation it is the oldest overdue date. */
  dueDate: IsoDate;
  name: string;
  /** Secondary line: when it is due, or why it cannot be paid. */
  when: string;
  amountCents: number;
  /** True when the account or category is inactive, so "Pagar" is inactive. */
  blocked: boolean;
}

/** Unique key of one occurrence: obligation id plus due date. */
function commitmentKey(obligationId: string, dueDate: IsoDate): string {
  return `${obligationId}:${dueDate}`;
}

/** Number of days the horizon covers, both ends included. */
export function horizonDays(dashboard: DashboardResponse): number {
  return daysBetween(dashboard.horizon.from, dashboard.horizon.to) + 1;
}

/** Text of the horizon chip: "Próximos 14 días · 20 oct". */
export function horizonLabel(dashboard: DashboardResponse): string {
  return `Próximos ${horizonDays(dashboard)} días · ${formatDayMonth(dashboard.horizon.to)}`;
}

/** Why a blocked obligation cannot be paid, from its PAYMENT_BLOCKED attention item. */
function blockedReason(item: PaymentBlockedItem | undefined): string {
  if (!item) {
    return 'No se puede pagar';
  }
  if (item.accountInactive && item.categoryInactive) {
    return 'Cuenta y categoría inactivas';
  }
  if (item.accountInactive) {
    return 'Cuenta inactiva';
  }
  if (item.categoryInactive) {
    return 'Categoría inactiva';
  }
  return 'No se puede pagar';
}

/**
 * Rows of "Próximos compromisos": overdue obligations first (in the server's attention order),
 * then the upcoming occurrences in the server's order. Overdue rows have the same weight as the
 * rest; only their date text changes.
 */
export function commitmentRows(dashboard: DashboardResponse): CommitmentRow[] {
  const today = dashboard.horizon.from;
  const blockedByObligation = new Map<string, PaymentBlockedItem>();
  const rows: CommitmentRow[] = [];

  for (const item of dashboard.attention) {
    if (item.type === 'PAYMENT_BLOCKED') {
      blockedByObligation.set(item.obligation.id, item);
    }
  }

  for (const item of dashboard.attention) {
    if (item.type !== 'OVERDUE_OCCURRENCE') {
      continue;
    }
    const since = formatDayMonth(item.oldestOverdue);
    // PAYMENT_BLOCKED covers overdue commitments too, not only the ones inside the horizon.
    const blockedItem = blockedByObligation.get(item.obligation.id);
    rows.push({
      key: commitmentKey(item.obligation.id, item.oldestOverdue),
      obligationId: item.obligation.id,
      dueDate: item.oldestOverdue,
      name: item.obligation.name,
      when: blockedItem
        ? blockedReason(blockedItem)
        : item.overdueCount > 1
          ? `${item.overdueCount} pendientes desde el ${since}`
          : `pendiente desde el ${since}`,
      // Exact: every overdue occurrence shares the obligation's current amount, and "Pagar"
      // settles only the oldest one.
      amountCents: Math.trunc(item.overdueAmountCents / item.overdueCount),
      blocked: blockedItem !== undefined,
    });
  }

  for (const commitment of dashboard.upcomingCommitments) {
    const blockedItem = blockedByObligation.get(commitment.obligation.id);
    rows.push({
      key: commitmentKey(commitment.obligation.id, commitment.dueDate),
      obligationId: commitment.obligation.id,
      dueDate: commitment.dueDate,
      name: commitment.obligation.name,
      when: commitment.paymentBlocked ? blockedReason(blockedItem) : formatDueDate(today, commitment.dueDate),
      amountCents: commitment.amountCents,
      blocked: commitment.paymentBlocked,
    });
  }

  return rows;
}

/** Operations of "Reciente": the newest ones, limited to what the design shows. */
export function recentOperations(dashboard: DashboardResponse): RecentActivityItem[] {
  return dashboard.recent.slice(0, recentLimit);
}

/** ACCOUNT_SHORTFALL items, in the server's attention order. */
export function accountShortfalls(dashboard: DashboardResponse): AccountShortfallItem[] {
  return dashboard.attention.filter((item): item is AccountShortfallItem => item.type === 'ACCOUNT_SHORTFALL');
}

/** Main text of the account notice for the first account that falls short. */
export function accountNoticeText(item: AccountShortfallItem, days: number): string {
  return `Tu cuenta ${item.account.name} podría no cubrir sus compromisos de los próximos ${days} días.`;
}

/** Toast text listing the other accounts that fall short, one sentence each. */
export function extraNoticesText(items: AccountShortfallItem[]): string {
  return items
    .map(
      (item) => `Tu cuenta ${item.account.name} podría no cubrir su pago del ${formatDayMonth(item.nearestDueDate)}.`,
    )
    .join(' ');
}

/**
 * Whether the user has any obligation at all, as the `select` of the obligations query. It only
 * tells a new user (invite them to add commitments) apart from a user with nothing due in the
 * horizon. A module-level function, so the query keeps a stable `select`.
 */
export function hasAnyObligation(obligations: ObligationResponse[]): boolean {
  return obligations.length > 0;
}

/** Toast text after paying a commitment: "Pagaste Renta · $8,000". */
export function paidToastText(row: CommitmentRow): string {
  return `Pagaste ${row.name} · ${formatMoney(row.amountCents)}`;
}

/** What the registration sheet registers: an expense ("+ Gasto") or an income ("Ingreso"). */
export type RegistrationType = 'EXPENSE' | 'INCOME';

/** Error under the sheet's amount when it is empty, $0, or negative. */
export const AMOUNT_REQUIRED_ERROR = 'Escribe un importe mayor a $0.';

/** Shown on "Guardar" while the accounts have not loaded yet. Not in the design. */
export const ACCOUNTS_NOT_LOADED_ERROR = 'Aún no cargamos tus cuentas. Intenta de nuevo.';

/** Shown on "Guardar" when the user has no active account to register in. Not in the design. */
export const NO_ACTIVE_ACCOUNT_ERROR = 'No tienes cuentas activas.';

/** Title of the registration sheet. */
export function registrationTitle(type: RegistrationType): string {
  return type === 'EXPENSE' ? 'Nuevo gasto' : 'Nuevo ingreso';
}

/** Toast after a registration: "Gasto registrado · $420" or "Ingreso registrado · $12,500". */
export function registeredToastText(type: RegistrationType, amountCents: number): string {
  return `${type === 'EXPENSE' ? 'Gasto' : 'Ingreso'} registrado · ${formatMoney(amountCents)}`;
}

/** Toast when the dashboard opens right after the onboarding: "{nombre} está lista.". */
export function arrivalToastText(accountName: string): string {
  return `${accountName} está lista.`;
}

/** Accounts that take new operations, in the API's order. */
export function activeAccounts(accounts: AccountResponse[]): AccountResponse[] {
  return accounts.filter((account) => account.status === 'ACTIVE');
}

/** Chips of the sheet's category choice: the active categories of that type, in the API's order. */
export function categoryOptions(categories: CategoryResponse[], type: RegistrationType): ChoiceOption<string>[] {
  return categories
    .filter((category) => category.type === type && category.status === 'ACTIVE')
    .map((category) => ({ value: category.id, label: category.name }));
}

/** Chips of the sheet's account choice, for a user with several active accounts. */
export function accountOptions(accounts: AccountResponse[]): ChoiceOption<string>[] {
  return accounts.map((account) => ({ value: account.id, label: account.name }));
}

/**
 * Account the sheet preselects: the last one used, which is the account of the newest income or
 * expense in the dashboard's recent activity, while it is still active; otherwise the first active
 * account. Nothing is stored on the device. `accounts` are the active ones (`activeAccounts`).
 * Undefined when there is no active account.
 */
export function defaultAccountId(recent: RecentActivityItem[], accounts: AccountResponse[]): string | undefined {
  const active = new Set(accounts.map((account) => account.id));
  // `recent` is newest first; transfers have no single account, so they are skipped.
  const lastUsed = recent.find((item) => item.type !== 'TRANSFER' && item.account !== null)?.account?.id;
  return lastUsed !== undefined && active.has(lastUsed) ? lastUsed : accounts[0]?.id;
}
