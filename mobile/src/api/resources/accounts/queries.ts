import { queryOptions } from '@tanstack/react-query';

import { queryKeys } from '../../client';

import { listAccounts } from './endpoints';

/**
 * Query options of the account list, for `useQuery(accountsQuery())`. Add a `select` to derive a
 * smaller value (e.g. whether the user has any account).
 */
export function accountsQuery() {
  return queryOptions({ queryKey: queryKeys.accounts, queryFn: listAccounts });
}
