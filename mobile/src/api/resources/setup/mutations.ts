import { useMutation, useQueryClient } from '@tanstack/react-query';

import { isApiError, queryKeys } from '../../client';

import { completeInitialSetup } from './endpoints';
import type { CompleteInitialSetupRequest } from './types';

/** True for the 409 that says the setup already ran. */
function isSetupAlreadyCompleted(error: unknown): boolean {
  return isApiError(error) && error.status === 409 && error.code === 'INITIAL_SETUP_ALREADY_COMPLETED';
}

/**
 * Runs the initial setup and refreshes the accounts, categories, and dashboard. A 409
 * `INITIAL_SETUP_ALREADY_COMPLETED` resolves as a success: a previous attempt already created the
 * account (e.g. the answer to a request that timed out never arrived). Never retried.
 */
export function useCompleteInitialSetup() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CompleteInitialSetupRequest): Promise<void> => {
      try {
        await completeInitialSetup(body);
      } catch (error) {
        if (!isSetupAlreadyCompleted(error)) {
          throw error;
        }
      }
    },
    retry: 0,
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.accounts }),
        queryClient.invalidateQueries({ queryKey: queryKeys.categories }),
        queryClient.invalidateQueries({ queryKey: queryKeys.dashboard }),
      ]),
  });
}
