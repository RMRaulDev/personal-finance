import type { AccountResponse } from '../../api';
import type { ParsedAmount } from '../../lib';

// Copy and validation rules of the onboarding, from the "Onboarding v1" design (Canvas sections 07
// and 08). Pure, so every rule of the flow is readable in one place.

/** Name the account field starts with; the user may keep, edit, or erase it. */
export const DEFAULT_ACCOUNT_NAME = 'Cuenta principal';

/** Longest account name the field accepts. Client-side only: the backend sets its own rules. */
export const ACCOUNT_NAME_MAX_LENGTH = 40;

/** Step 1 error when the trimmed name is empty. */
export const NAME_EMPTY_ERROR = 'Escribe un nombre para tu cuenta. Por ejemplo, «Cuenta principal».';

/** Step 2 help text. */
const BALANCE_HELP = 'Lo que ves hoy en la app de tu banco o en tu cartera.';
/** Step 2 help text when the balance is exactly $0. */
const BALANCE_ZERO_HELP = 'Empezar en $0 está bien. Lo que registres se sumará desde aquí.';
/** Step 2 error after tapping "Empezar" with nothing typed. */
const BALANCE_EMPTY_ERROR = 'Escribe cuánto tienes hoy. Si esta cuenta está en ceros, escribe 0.';
/** Step 2 error shown while the typed balance is negative. */
const BALANCE_NEGATIVE_ERROR =
  'El saldo de inicio va de $0 en adelante. Si es una tarjeta con saldo por pagar, empieza con la cuenta donde recibes tu dinero.';

/**
 * Whether the app should open the onboarding: the user has no account yet. The `select` of the
 * accounts query at launch; a module-level function, so the query keeps a stable `select`.
 */
export function needsOnboarding(accounts: AccountResponse[]): boolean {
  return accounts.length === 0;
}

/** Step 2 title: "¿Cuánto tienes hoy en {nombre}?", with "tu cuenta" when the name is empty. */
export function balanceTitle(accountName: string): string {
  return `¿Cuánto tienes hoy en ${accountName === '' ? 'tu cuenta' : accountName}?`;
}

/** Help or error under the balance field: at most one of them is set. */
export interface BalanceFeedback {
  help?: string;
  error?: string;
}

/**
 * What step 2 says under the balance field. A negative value is explained as soon as it is typed;
 * the empty error only appears after tapping "Empezar" (`emptySubmitted`) and leaves on the next
 * keystroke; $0 changes the help text so the user knows it is accepted.
 */
export function balanceFeedback(amount: ParsedAmount, emptySubmitted: boolean): BalanceFeedback {
  if (amount.kind === 'negative') {
    return { error: BALANCE_NEGATIVE_ERROR };
  }
  if (amount.kind === 'empty') {
    return emptySubmitted ? { error: BALANCE_EMPTY_ERROR } : { help: BALANCE_HELP };
  }
  return { help: amount.cents === 0 ? BALANCE_ZERO_HELP : BALANCE_HELP };
}
