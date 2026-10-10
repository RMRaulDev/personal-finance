import type { AccountSummary, CategorySummary, IsoDate } from '../common';

// Types of the obligations resource: `/api/v1/obligations` and their occurrences (`entry.web.obligation`).

/** Recurrence frequency of an obligation. */
export type Frequency = 'ONCE' | 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'YEARLY';

/** Calendar of an obligation; `endDate` is null when it has no end. */
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
  status: 'ACTIVE' | 'ARCHIVED';
}

/** Response of paying an occurrence: the id of the expense it registered. */
export interface PayOccurrenceResponse {
  operationId: string;
}

/** Identifies the occurrence to pay. */
export interface PayOccurrenceVariables {
  obligationId: string;
  dueDate: IsoDate;
}
