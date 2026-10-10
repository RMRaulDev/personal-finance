/** Cents in one peso; money is always handled as integer cents. */
export const CENTS_PER_PESO = 100;

/** Formats integer pesos with thousands separators (`8000` → `8,000`). */
const pesosFormatter = new Intl.NumberFormat('es-MX', { maximumFractionDigits: 0, useGrouping: true });

/**
 * Formats a non-negative amount in integer cents as MXN for display: `$8,000`, or `$1,234.50`
 * when the amount has cents. Callers add the sign when they need one.
 */
export function formatMoney(cents: number): string {
  // Integer math only: money never goes through floating-point decimals.
  const pesos = Math.trunc(cents / CENTS_PER_PESO);
  const rest = cents % CENTS_PER_PESO;
  const whole = `$${pesosFormatter.format(pesos)}`;
  // The design shows whole amounts without decimals ("$4,800").
  return rest === 0 ? whole : `${whole}.${String(rest).padStart(2, '0')}`;
}
