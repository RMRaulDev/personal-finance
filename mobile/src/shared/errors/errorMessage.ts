import { isApiError, type BusinessRuleCode } from '../../api';

/**
 * User copy for the 409 codes the app can receive. This is the single error copy table: every
 * feature shows API errors through `errorMessage`, so the same code always reads the same.
 */
const CONFLICT_MESSAGES: Partial<Record<BusinessRuleCode, string>> = {
  OCCURRENCE_ALREADY_RESOLVED: 'Ese compromiso ya estaba resuelto.',
  OCCURRENCE_NOT_SCHEDULED: 'Esa fecha ya no está en el calendario del compromiso.',
  OBLIGATION_ARCHIVED: 'Ese compromiso está archivado.',
  // Neutral texts: payments and registrations share them.
  ACCOUNT_INACTIVE: 'La cuenta está inactiva.',
  CATEGORY_INACTIVE: 'La categoría está inactiva.',
  INSUFFICIENT_BALANCE: 'No hay saldo suficiente en la cuenta.',
  OPERATION_ALREADY_CANCELLED: 'Esa operación ya estaba cancelada.',
};

const GENERIC_MESSAGE = 'Algo salió mal. Intenta de nuevo.';

/** Returns true when `code` is one of the codes the table has copy for. */
function hasConflictMessage(code: string): code is keyof typeof CONFLICT_MESSAGES {
  // Own keys only: `in` would also match inherited names such as "constructor".
  return Object.hasOwn(CONFLICT_MESSAGES, code);
}

/**
 * Chooses the copy shown for a failed request from its kind, status, and code. The server's
 * `detail` is never shown: it is written for developers, not users.
 */
export function errorMessage(error: unknown): string {
  if (!isApiError(error)) {
    return GENERIC_MESSAGE;
  }
  if (error.kind === 'not-configured') {
    return 'Configura EXPO_PUBLIC_API_URL en mobile/.env.local.';
  }
  if (error.kind === 'network') {
    return 'No se pudo conectar con el servidor.';
  }
  if (error.status === 409 && error.code && hasConflictMessage(error.code)) {
    return CONFLICT_MESSAGES[error.code] ?? GENERIC_MESSAGE;
  }
  return GENERIC_MESSAGE;
}
