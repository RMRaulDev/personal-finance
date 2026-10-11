import type { AccountResponse, RecentActivityItem } from '../../../../api';
import { formatMoney } from '../../../../lib';

// Pure rules of the registration sheet ("Nuevo gasto" / "Nuevo ingreso"): its texts, errors, and
// default account, kept out of the components so they read in one place. The account and category
// choices are shared with other forms (`shared` → `choiceOptions`).

/** What the registration sheet registers: an expense ("+ Gasto") or an income ("Ingreso"). */
export type RegistrationType = 'EXPENSE' | 'INCOME';

/** Error under the sheet's amount when it is empty, $0, or negative. */
export const AMOUNT_REQUIRED_ERROR = 'Escribe un importe mayor a $0.';

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

/**
 * Account the sheet preselects: the last one used, which is the account of the newest income or
 * expense in the dashboard's recent activity, while it is still active; otherwise the first active
 * account. Nothing is stored on the device. `accounts` are the active ones (`activeAccounts` in shared).
 * Undefined when there is no active account.
 */
export function defaultAccountId(recent: RecentActivityItem[], accounts: AccountResponse[]): string | undefined {
  const active = new Set(accounts.map((account) => account.id));
  // `recent` is newest first; transfers have no single account, so they are skipped.
  const lastUsed = recent.find((item) => item.type !== 'TRANSFER' && item.account !== null)?.account?.id;
  return lastUsed !== undefined && active.has(lastUsed) ? lastUsed : accounts[0]?.id;
}
