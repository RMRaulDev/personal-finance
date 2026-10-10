import { queryOptions } from '@tanstack/react-query';

import { queryKeys } from '../../client';

import { listObligations } from './endpoints';

/**
 * Query options of the obligation list, for `useQuery(obligationsQuery())`. Add a `select` to
 * derive a smaller value (e.g. whether the user has any obligation).
 */
export function obligationsQuery() {
  return queryOptions({ queryKey: queryKeys.obligations, queryFn: listObligations });
}
