import { queryOptions } from '@tanstack/react-query';

import { request } from './client/client';
import { queryKeys } from './client/queryClient';

// The accounts resource: `/api/v1/accounts` (`entry.web.account`).

/** Status of an account. Only active accounts take new operations. */
export type AccountStatus = 'ACTIVE' | 'INACTIVE';

/** One account, from `GET /accounts`. `openingBalanceCents` is the balance it started with. */
export interface AccountResponse {
  id: string;
  name: string;
  balanceCents: number;
  openingBalanceCents: number;
  status: AccountStatus;
}

/** `GET /accounts`: every account of the user, active and inactive, ordered by name then id. */
export function listAccounts(): Promise<AccountResponse[]> {
  return request<AccountResponse[]>('GET', '/accounts');
}

/**
 * Query options of the account list, for `useQuery(accountsQuery())`. Add a `select` to derive a
 * smaller value (e.g. whether the user has any account).
 */
export function accountsQuery() {
  return queryOptions({ queryKey: queryKeys.accounts, queryFn: listAccounts });
}
