import { request } from '../../client';

import type { AccountResponse } from './types';

// Endpoints of the accounts resource: `/api/v1/accounts` (`entry.web.account`).

/** `GET /accounts`: every account of the user, active and inactive, ordered by name then id. */
export function listAccounts(): Promise<AccountResponse[]> {
  return request<AccountResponse[]>('GET', '/accounts');
}
