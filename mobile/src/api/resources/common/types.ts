// Types shared by several resources of the REST API under /api/v1. They mirror the backend web DTOs
// (entry.web.common) and keep the API's names. Money is always an integer number of cents
// (`...Cents`), dates are ISO-8601 calendar dates (`"2026-10-07"`), ids are UUID strings, and enums
// are uppercase strings.

/** ISO-8601 calendar date without time zone, e.g. `"2026-10-07"`. */
export type IsoDate = string;

/** `{id, name}` reference to an account (`AccountSummaryResponse`). */
export interface AccountSummary {
  id: string;
  name: string;
}

/** `{id, name}` reference to a category (`CategorySummaryResponse`). */
export interface CategorySummary {
  id: string;
  name: string;
}

/** `{id, name}` reference to an obligation (`ObligationSummaryResponse`). */
export interface ObligationSummary {
  id: string;
  name: string;
}

/** Source and target accounts of a transfer (`TransferResponse`). */
export interface TransferAccounts {
  sourceAccount: AccountSummary;
  targetAccount: AccountSummary;
}

/** Type of a financial operation. */
export type OperationType = 'INCOME' | 'EXPENSE' | 'TRANSFER';
