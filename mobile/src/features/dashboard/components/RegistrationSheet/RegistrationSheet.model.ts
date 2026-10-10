import type { AccountResponse, CategoryResponse, RecentActivityItem } from '../../../../api';
import type { ChoiceOption } from '../../../../design-system';
import { formatMoney } from '../../../../lib';

// Pure rules of the registration sheet ("Nuevo gasto" / "Nuevo ingreso"): its texts, errors, and
// choices, kept out of the components so they read in one place.

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
