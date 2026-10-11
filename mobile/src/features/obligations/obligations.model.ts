import {
  isApiError,
  type AccountResponse,
  type CategoryResponse,
  type IsoDate,
  type ObligationResponse,
} from '../../api';
import { formatFrequency } from '../../lib';
import { errorMessage } from '../../shared';

// Rules shared by the screens of the obligations feature ("compromisos" in the UI): texts that
// several screens show the same way, and the blocked state. Each screen's own texts live in its
// `.model.ts`.

/** Picks the singular or plural form of a word for a count. */
export function plural(count: number, one: string, many: string): string {
  return count === 1 ? one : many;
}

/** Frequency of an obligation in words ("Mensual · día 5"), relative to the server's `today`. */
export function frequencyText(obligation: ObligationResponse, today: IsoDate): string {
  return formatFrequency(today, obligation.recurrence.frequency, obligation.recurrence.startDate);
}

/**
 * Why an obligation cannot be paid with its own account and category, or null when it can: its
 * category or its account is no longer active (read from the account and category lists, which
 * carry the statuses). The category is checked first, as the design does. A list that is undefined
 * (not loaded) rules nothing out, so null then means "not known to be blocked"; a screen that must
 * not read it as "not blocked" checks that both lists loaded first.
 */
export function blockedReason(
  obligation: ObligationResponse,
  accounts: AccountResponse[] | undefined,
  categories: CategoryResponse[] | undefined,
): string | null {
  const category = categories?.find((item) => item.id === obligation.category.id);
  if (category !== undefined && category.status !== 'ACTIVE') {
    return `La categoría «${obligation.category.name}» ya no está activa.`;
  }
  const account = accounts?.find((item) => item.id === obligation.account.id);
  if (account !== undefined && account.status !== 'ACTIVE') {
    return `La cuenta «${obligation.account.name}» ya no está activa.`;
  }
  return null;
}

/**
 * What to do after a screen failed to load: the design's text for a lost connection ("Revisa tu
 * conexión…"), the shared error copy otherwise.
 */
export function loadFailureMessage(error: unknown): string {
  return isApiError(error) && error.kind === 'network'
    ? 'Revisa tu conexión y vuelve a intentarlo. No se perdió nada de lo que ya registraste.'
    : errorMessage(error);
}
