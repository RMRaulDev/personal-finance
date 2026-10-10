import type {
  AccountSummary,
  CategorySummary,
  IsoDate,
  ObligationSummary,
  OperationType,
  TransferAccounts,
} from '../api';
import { formatMoney, formatRelativeDay } from '../lib';

/**
 * The fields of an operation that `operationDisplay` reads. Structural, so the dashboard's recent
 * activity and a future history item both fit; `obligation` is optional because only some
 * responses carry it.
 */
export interface OperationDisplayInput {
  type: OperationType;
  amountCents: number;
  operationDate: IsoDate;
  account: AccountSummary | null;
  category: CategorySummary | null;
  transfer: TransferAccounts | null;
  obligation?: ObligationSummary | null;
}

/** Text tone of an operation's amount and glyph: a design system text color. */
export type OperationTone = 'success' | 'primary';

/** How an operation is shown in a row. */
export interface OperationDisplay {
  /** "−" for expenses, "+" for incomes, "⇄" for transfers. */
  glyph: string;
  label: string;
  /** Relative day and account(s), e.g. "Hoy · Nómina". */
  meta: string;
  /** Signed amount text, e.g. "−$420" or "+$12,500"; transfers have no sign. */
  amount: string;
  /** `success` only for incomes; the sign and glyph say it too, so color is never the only cue. */
  tone: OperationTone;
}

/**
 * Maps an operation to its row texts, relative to `today` (the server's, never the device's).
 * The label is the paid obligation, else the category, else the operation type.
 */
export function operationDisplay(operation: OperationDisplayInput, today: IsoDate): OperationDisplay {
  const day = formatRelativeDay(today, operation.operationDate);
  const amount = formatMoney(operation.amountCents);

  if (operation.type === 'TRANSFER') {
    const accounts = operation.transfer
      ? `${operation.transfer.sourceAccount.name} → ${operation.transfer.targetAccount.name}`
      : 'Transferencia';
    return { glyph: '⇄', label: 'Transferencia', meta: `${day} · ${accounts}`, amount, tone: 'primary' };
  }

  const income = operation.type === 'INCOME';
  // A paid obligation names the expense better than its category ("Renta" instead of "Vivienda").
  const label = operation.obligation?.name ?? operation.category?.name ?? (income ? 'Ingreso' : 'Gasto');
  const meta = operation.account ? `${day} · ${operation.account.name}` : day;
  return {
    glyph: income ? '+' : '−',
    label,
    meta,
    amount: `${income ? '+' : '−'}${amount}`,
    tone: income ? 'success' : 'primary',
  };
}
