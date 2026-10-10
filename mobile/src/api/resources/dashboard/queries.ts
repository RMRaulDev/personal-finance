import { queryOptions } from '@tanstack/react-query';

import { queryKeys } from '../../client';

import { getDashboard } from './endpoints';

/** Query options of the dashboard, for `useQuery(dashboardQuery())`. */
export function dashboardQuery() {
  return queryOptions({ queryKey: queryKeys.dashboard, queryFn: getDashboard });
}
