import { queryOptions } from '@tanstack/react-query';

import { queryKeys } from '../../client';

import { getObligation, listObligationOccurrences, listObligations } from './endpoints';

/**
 * Query options of the obligation list, for `useQuery(obligationsQuery())`. Add a `select` to
 * derive a smaller value (e.g. how many obligations the user has).
 */
export function obligationsQuery() {
  return queryOptions({ queryKey: queryKeys.obligations, queryFn: listObligations });
}

/** Query options of one obligation, for `useQuery(obligationQuery(id))`. */
export function obligationQuery(obligationId: string) {
  return queryOptions({ queryKey: queryKeys.obligation(obligationId), queryFn: () => getObligation(obligationId) });
}

/** Query options of one obligation's occurrences, for `useQuery(obligationOccurrencesQuery(id))`. */
export function obligationOccurrencesQuery(obligationId: string) {
  return queryOptions({
    queryKey: queryKeys.obligationOccurrences(obligationId),
    queryFn: () => listObligationOccurrences(obligationId),
  });
}
