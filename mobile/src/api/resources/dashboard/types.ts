import type {
  AccountSummary,
  CategorySummary,
  IsoDate,
  ObligationSummary,
  OperationType,
  TransferAccounts,
} from '../common';

// Types of the dashboard resource: `GET /api/v1/dashboard` (`entry.web.dashboard`).

/** Dates the dashboard covers: `from` is the server's "today", `to` is inclusive. */
export interface Horizon {
  from: IsoDate;
  to: IsoDate;
}

/** Available-to-spend figures; `available` and `shortfall` are never negative. */
export interface AvailableToSpend {
  balanceCents: number;
  committedCents: number;
  availableCents: number;
  shortfallCents: number;
}

/** An obligation with one or more pending occurrences before today. */
export interface OverdueOccurrenceItem {
  type: 'OVERDUE_OCCURRENCE';
  obligation: ObligationSummary;
  oldestOverdue: IsoDate;
  overdueCount: number;
  overdueAmountCents: number;
}

/** The total balance does not cover the commitments in the horizon. */
export interface ShortfallItem {
  type: 'SHORTFALL';
  shortfallCents: number;
}

/** An obligation that cannot be paid because its account or category is inactive. */
export interface PaymentBlockedItem {
  type: 'PAYMENT_BLOCKED';
  obligation: ObligationSummary;
  account: AccountSummary;
  nearestDueDate: IsoDate;
  committedCents: number;
  accountInactive: boolean;
  categoryInactive: boolean;
}

/** One account does not cover its own commitments in the horizon. */
export interface AccountShortfallItem {
  type: 'ACCOUNT_SHORTFALL';
  account: AccountSummary;
  shortfallCents: number;
  nearestDueDate: IsoDate;
}

/** An item that needs the user's attention, discriminated by `type`. */
export type AttentionItem = OverdueOccurrenceItem | ShortfallItem | PaymentBlockedItem | AccountShortfallItem;

/** A pending occurrence due inside the horizon. */
export interface UpcomingCommitment {
  obligation: ObligationSummary;
  dueDate: IsoDate;
  amountCents: number;
  account: AccountSummary;
  paymentBlocked: boolean;
}

/**
 * A recent active operation. Incomes and expenses have `account` and a null `transfer`; their
 * `category` is null when they were registered without one. Transfers have `transfer` and null
 * `account` and `category`. `obligation` is set when the expense paid an obligation.
 */
export interface RecentActivityItem {
  id: string;
  type: OperationType;
  amountCents: number;
  operationDate: IsoDate;
  account: AccountSummary | null;
  category: CategorySummary | null;
  transfer: TransferAccounts | null;
  obligation: ObligationSummary | null;
}

/** Response of `GET /dashboard`. */
export interface DashboardResponse {
  horizon: Horizon;
  availableToSpend: AvailableToSpend;
  attention: AttentionItem[];
  upcomingCommitments: UpcomingCommitment[];
  recent: RecentActivityItem[];
}
