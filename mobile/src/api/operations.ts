import { useMutation, useQueryClient } from '@tanstack/react-query';

import { request } from './client';
import { invalidateMoneyMovement } from './invalidation';

// The operations resource: `/api/v1/operations` (`entry.web.operation`).

/** `POST /operations/{id}/cancel`: cancels an income or expense by registering its reversal. */
export function cancelOperation(operationId: string): Promise<void> {
  return request<void>('POST', `/operations/${encodeURIComponent(operationId)}/cancel`);
}

/**
 * Cancels an operation (e.g. to undo a payment) and refreshes every query a money movement
 * changes. Never retried.
 */
export function useCancelOperation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (operationId: string) => cancelOperation(operationId),
    retry: 0,
    onSuccess: () => invalidateMoneyMovement(queryClient),
  });
}
