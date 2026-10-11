import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';

import { invalidateMoneyMovement, invalidateObligations, isApiError } from '../../client';

import {
  archiveObligation,
  createObligation,
  modifyObligation,
  payOccurrence,
  reopenOccurrence,
  skipOccurrence,
  skipOverdueOccurrences,
} from './endpoints';
import type { ModifyObligationVariables, OccurrenceVariables, PayOccurrenceVariables } from './types';

// Mutations of the obligations resource. None is ever retried: payments are not idempotent, and a
// retried create could fail as a duplicate name after the first attempt succeeded.

/**
 * Refreshes the obligations and the dashboard when an occurrence turns out to be resolved already
 * (409 `OCCURRENCE_ALREADY_RESOLVED`, e.g. from another device), so the screen shows its real state.
 */
function refreshIfAlreadyResolved(queryClient: QueryClient, error: unknown): Promise<unknown> | undefined {
  if (isApiError(error) && error.status === 409 && error.code === 'OCCURRENCE_ALREADY_RESOLVED') {
    return invalidateObligations(queryClient);
  }
  return undefined;
}

/** Creates an obligation and refreshes the obligations and the dashboard. Never retried. */
export function useCreateObligation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createObligation,
    retry: 0,
    onSuccess: () => invalidateObligations(queryClient),
  });
}

/** Changes an obligation and refreshes the obligations and the dashboard. Never retried. */
export function useModifyObligation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ obligationId, changes }: ModifyObligationVariables) => modifyObligation(obligationId, changes),
    retry: 0,
    onSuccess: () => invalidateObligations(queryClient),
  });
}

/** Archives an obligation and refreshes the obligations and the dashboard. Never retried. */
export function useArchiveObligation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (obligationId: string) => archiveObligation(obligationId),
    retry: 0,
    onSuccess: () => invalidateObligations(queryClient),
  });
}

/**
 * Pays one occurrence, with its defaults or with the changes in `body`, and refreshes every query a
 * money movement changes. Never retried: a retry after a timeout could pay it twice.
 */
export function usePayOccurrence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ obligationId, dueDate, body }: PayOccurrenceVariables) => payOccurrence(obligationId, dueDate, body),
    retry: 0,
    onSuccess: () => invalidateMoneyMovement(queryClient),
    onError: (error) => refreshIfAlreadyResolved(queryClient, error),
  });
}

/** Skips one occurrence and refreshes the obligations and the dashboard. Never retried. */
export function useSkipOccurrence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ obligationId, dueDate }: OccurrenceVariables) => skipOccurrence(obligationId, dueDate),
    retry: 0,
    onSuccess: () => invalidateObligations(queryClient),
    onError: (error) => refreshIfAlreadyResolved(queryClient, error),
  });
}

/** Reopens a skipped occurrence and refreshes the obligations and the dashboard. Never retried. */
export function useReopenOccurrence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ obligationId, dueDate }: OccurrenceVariables) => reopenOccurrence(obligationId, dueDate),
    retry: 0,
    onSuccess: () => invalidateObligations(queryClient),
  });
}

/**
 * Skips every overdue occurrence of an obligation and refreshes the obligations and the dashboard.
 * Resolves with the skipped due dates, so they can be reopened one by one to undo it. Never retried.
 */
export function useSkipOverdueOccurrences() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (obligationId: string) => skipOverdueOccurrences(obligationId),
    retry: 0,
    onSuccess: () => invalidateObligations(queryClient),
  });
}
