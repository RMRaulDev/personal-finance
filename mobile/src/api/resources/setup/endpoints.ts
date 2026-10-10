import { request } from '../../client';

import type { CompleteInitialSetupRequest, CompleteInitialSetupResponse } from './types';

// Endpoint of the initial setup: `POST /api/v1/setup` (`entry.web.setup`).

/**
 * `POST /setup`: creates the first account and the default categories in one transaction. Answers
 * 409 `INITIAL_SETUP_ALREADY_COMPLETED` when the user already has any account or category.
 */
export function completeInitialSetup(body: CompleteInitialSetupRequest): Promise<CompleteInitialSetupResponse> {
  return request<CompleteInitialSetupResponse>('POST', '/setup', body);
}
