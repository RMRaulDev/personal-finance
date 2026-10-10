const CENTS_PER_PESO = 100;

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

/** Most digits an amount input keeps (whole and decimal together); later digits are ignored. */
const MAX_AMOUNT_DIGITS = 11;
/** Most decimal digits an amount input keeps; a third one is ignored. */
const MAX_AMOUNT_DECIMALS = 2;

/** An amount input split into its parts, without separators. */
interface AmountInputParts {
  negative: boolean;
  /** Whole pesos as typed, without leading zeros ("" when nothing was typed before the point). */
  whole: string;
  /** True once a decimal point was typed, so "12." keeps its point while typing. */
  point: boolean;
  /** Up to two decimal digits. */
  decimals: string;
}

/**
 * Cleans typed or pasted text into its amount parts: keeps the digits and the first decimal
 * point, drops everything else ("$", spaces, the thousands commas this module adds), keeps at most
 * 11 digits and 2 decimals, and remembers a "-" before the digits so a pasted negative is shown
 * and explained instead of silently dropped.
 *
 * A trailing "," in text without a "." is read as the decimal point: some decimal keypads (other
 * regions) type "," for it, and the formatted text never ends in a comma, so a trailing one can
 * only be a keystroke. Once it becomes "." the next digits are decimals ("12," → "12." → "12.5").
 */
function cleanAmountInput(rawText: string): AmountInputParts {
  const text = rawText.endsWith(',') && !rawText.includes('.') ? `${rawText.slice(0, -1)}.` : rawText;
  // A "-" before the first digit is a sign ("-500", "$-500"); one after it is not.
  const firstDigit = text.search(/\d/);
  const negative = text.slice(0, firstDigit === -1 ? text.length : firstDigit).includes('-');
  let whole = '';
  let decimals = '';
  let point = false;
  let digits = 0;

  for (const char of text) {
    if (char >= '0' && char <= '9') {
      if (digits >= MAX_AMOUNT_DIGITS || (point && decimals.length >= MAX_AMOUNT_DECIMALS)) {
        continue;
      }
      if (point) {
        decimals += char;
      } else {
        whole += char;
      }
      digits += 1;
    } else if (char === '.' && !point) {
      point = true;
    }
  }

  // "007" reads as "7"; a lone "0" stays, so the user can type "0" or "0.50".
  whole = whole.replace(/^0+(?=\d)/, '');
  return { negative, whole, point, decimals };
}

/**
 * Text to show in an amount field after each keystroke or paste: the cleaned input with thousands
 * separators (`"1234.5"` → `"1,234.5"`). An unfinished value keeps its shape (`"12."`, `"-"`).
 */
export function formatAmountInput(text: string): string {
  const { negative, whole, point, decimals } = cleanAmountInput(text);
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  // A point typed first reads as "0." so the value never starts with a bare point.
  const wholeText = point && grouped === '' ? '0' : grouped;
  return `${negative ? '-' : ''}${wholeText}${point ? `.${decimals}` : ''}`;
}

/** An amount input read as money: nothing typed, a negative value, or a valid amount in cents. */
export type ParsedAmount = { kind: 'empty' } | { kind: 'negative' } | { kind: 'ok'; cents: number };

/**
 * Reads an amount field's text (grouped or not) as integer cents, with integer math only: never
 * `parseFloat`. Any leading "-" makes it `negative`, so the caller can explain it.
 */
export function parseAmountInput(text: string): ParsedAmount {
  const { negative, whole, point, decimals } = cleanAmountInput(text);
  if (negative) {
    return { kind: 'negative' };
  }
  // A lone point is shown as "0.", so it reads as $0, not as nothing typed.
  if (whole === '' && !point) {
    return { kind: 'empty' };
  }
  const pesos = Number(whole === '' ? '0' : whole);
  const cents = Number(decimals.padEnd(MAX_AMOUNT_DECIMALS, '0'));
  // At most 11 digits, so the result stays far below Number.MAX_SAFE_INTEGER.
  return { kind: 'ok', cents: pesos * CENTS_PER_PESO + cents };
}
