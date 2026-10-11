import type { AccountSummary, CategorySummary, IsoDate } from '../common';

// Types of the obligations resource: `/api/v1/obligations` and their occurrences (`entry.web.obligation`).

/** Recurrence frequency of an obligation. */
export type Frequency = 'ONCE' | 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'YEARLY';

/** Status of an obligation. An archived one no longer counts and cannot be edited or paid. */
export type ObligationStatus = 'ACTIVE' | 'ARCHIVED';

/** Calendar of an obligation; `endDate` (inclusive) is null when it has no end. */
export interface Recurrence {
  frequency: Frequency;
  startDate: IsoDate;
  endDate: IsoDate | null;
}

/** One obligation, from `GET /obligations` and `GET /obligations/{id}`. */
export interface ObligationResponse {
  id: string;
  name: string;
  amountCents: number;
  account: AccountSummary;
  category: CategorySummary;
  recurrence: Recurrence;
  status: ObligationStatus;
  /**
   * Oldest unresolved occurrence: the oldest overdue date, or else the next pending one. Null when
   * nothing is left to resolve or the obligation is archived.
   */
  nextDueDate: IsoDate | null;
  /** Overdue occurrences; 0 when archived. */
  overdueCount: number;
  /** Day it was archived; null while active, and for obligations archived before it was recorded. */
  archivedOn: IsoDate | null;
}

/** Status of one occurrence: before today and unresolved, today or later and unresolved, or resolved. */
export type OccurrenceStatus = 'OVERDUE' | 'PENDING' | 'PAID' | 'SKIPPED';

/** The expense that paid an occurrence. */
export interface OccurrenceExpense {
  id: string;
  amountCents: number;
  operationDate: IsoDate;
}

/** One occurrence of an obligation; `expense` is set only when `PAID`. */
export interface Occurrence {
  dueDate: IsoDate;
  status: OccurrenceStatus;
  expense: OccurrenceExpense | null;
}

/**
 * Response of `GET /obligations/{id}/occurrences`: every overdue occurrence and the next 3 pending
 * ones (both ascending), and the last 4 resolutions by due date descending (advance payments
 * included). An archived obligation has empty `overdue` and `upcoming`.
 */
export interface ObligationOccurrencesResponse {
  overdue: Occurrence[];
  upcoming: Occurrence[];
  resolved: Occurrence[];
}

/** Calendar sent when creating or changing an obligation. A null `endDate` means no end. */
export interface RecurrenceRequest {
  frequency: Frequency;
  startDate: IsoDate;
  endDate: IsoDate | null;
}

/** Body of `POST /obligations`. */
export interface CreateObligationRequest {
  name: string;
  amountCents: number;
  accountId: string;
  categoryId: string;
  recurrence: RecurrenceRequest;
}

/**
 * Body of `PATCH /obligations/{id}`: only the fields that change (at least one). A `recurrence`
 * replaces the whole calendar.
 */
export interface ModifyObligationRequest {
  name?: string;
  amountCents?: number;
  accountId?: string;
  categoryId?: string;
  recurrence?: RecurrenceRequest;
}

/** Response of `POST /obligations`: the id of the new obligation. */
export interface CreateObligationResponse {
  id: string;
}

/**
 * Optional body of paying an occurrence: each field replaces the obligation's default for this
 * payment only. Without a body it is paid with the obligation's amount, account, and category, today.
 */
export interface PayOccurrenceRequest {
  amountCents?: number;
  operationDate?: IsoDate;
  accountId?: string;
  categoryId?: string;
}

/** Response of paying an occurrence: the id of the expense it registered. */
export interface PayOccurrenceResponse {
  operationId: string;
}

/** Identifies one occurrence: its obligation and due date. */
export interface OccurrenceVariables {
  obligationId: string;
  dueDate: IsoDate;
}

/** The occurrence to pay and, optionally, what changes for this payment. */
export interface PayOccurrenceVariables extends OccurrenceVariables {
  body?: PayOccurrenceRequest;
}

/** The obligation to change and the fields that change. */
export interface ModifyObligationVariables {
  obligationId: string;
  changes: ModifyObligationRequest;
}

/** Response of skipping every overdue occurrence: the due dates it skipped (`[]` when none). */
export interface SkipOverdueOccurrencesResponse {
  skippedDueDates: IsoDate[];
}
