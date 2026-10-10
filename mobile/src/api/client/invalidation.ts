import type { QueryClient } from '@tanstack/react-query';

import { queryKeys } from './queryClient';

/**
 * Invalidates every query whose data a money movement changes: the dashboard, obligations,
 * operations, and accounts. Every mutation that moves money (registrations, payments,
 * cancellations) uses this one helper, so none of them can forget a screen and leave a stale
 * balance, commitment, or history on display. Resolves when the active queries have refetched.
 */
export function invalidateMoneyMovement(queryClient: QueryClient): Promise<unknown> {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.dashboard }),
    queryClient.invalidateQueries({ queryKey: queryKeys.obligations }),
    queryClient.invalidateQueries({ queryKey: queryKeys.operations }),
    queryClient.invalidateQueries({ queryKey: queryKeys.accounts }),
  ]);
}
