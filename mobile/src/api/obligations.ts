import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';

import { request } from './client';
import type { AccountSummary, CategorySummary, IsoDate } from './common';
import { invalidateMoneyMovement } from './invalidation';
import { queryKeys } from './queryClient';

// The obligations resource: `/api/v1/obligations` and their occurrences (`entry.web.obligation`).

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

/** `GET /obligations`: every obligation of the user, active and archived. */
export function listObligations(): Promise<ObligationResponse[]> {
  return request<ObligationResponse[]>('GET', '/obligations');
}

/**
 * `POST /obligations/{id}/occurrences/{dueDate}/pay` without a body: pays the occurrence with the
 * obligation's amount, account, and category, dated today. Not idempotent beyond the server's
 * `OCCURRENCE_ALREADY_RESOLVED` guard.
 */
export function payOccurrence(obligationId: string, dueDate: IsoDate): Promise<PayOccurrenceResponse> {
  return request<PayOccurrenceResponse>(
    'POST',
    `/obligations/${encodeURIComponent(obligationId)}/occurrences/${encodeURIComponent(dueDate)}/pay`,
  );
}

/**
 * Query options of the obligation list, for `useQuery(obligationsQuery())`. Add a `select` to
 * derive a smaller value (e.g. whether the user has any obligation).
 */
export function obligationsQuery() {
  return queryOptions({ queryKey: queryKeys.obligations, queryFn: listObligations });
}

/**
 * Pays one occurrence with its defaults and refreshes every query a money movement changes. Never
 * retried: a retry after a timeout could pay it twice.
 */
export function usePayOccurrence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ obligationId, dueDate }: PayOccurrenceVariables) => payOccurrence(obligationId, dueDate),
    retry: 0,
    onSuccess: () => invalidateMoneyMovement(queryClient),
  });
}
