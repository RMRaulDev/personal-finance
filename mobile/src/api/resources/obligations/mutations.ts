import { useMutation, useQueryClient } from '@tanstack/react-query';

import { invalidateMoneyMovement } from '../../client';

import { payOccurrence } from './endpoints';
import type { PayOccurrenceVariables } from './types';

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
