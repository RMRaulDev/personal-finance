import { useMutation, useQueryClient } from '@tanstack/react-query';

import { request } from '../../client/client';
import { isApiError } from '../../client/errors';
import { queryKeys } from '../../client/queryClient';

// The initial setup: `POST /api/v1/setup` (`entry.web.setup`).

/** Body of `POST /setup`: the first account and its opening balance (0 or more). */
export interface CompleteInitialSetupRequest {
  accountName: string;
  openingBalanceCents: number;
}

/** Response of `POST /setup`: the id of the account it created. */
export interface CompleteInitialSetupResponse {
  id: string;
}

/**
 * `POST /setup`: creates the first account and the default categories in one transaction. Answers
 * 409 `INITIAL_SETUP_ALREADY_COMPLETED` when the user already has any account or category.
 */
export function completeInitialSetup(body: CompleteInitialSetupRequest): Promise<CompleteInitialSetupResponse> {
  return request<CompleteInitialSetupResponse>('POST', '/setup', body);
}

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
