import { queryOptions } from '@tanstack/react-query';

import { queryKeys } from '../../client';

import { getDashboard } from './endpoints';
import type { DashboardResponse, Horizon } from './types';

/** Query options of the dashboard, for `useQuery(dashboardQuery())`. */
export function dashboardQuery() {
  return queryOptions({ queryKey: queryKeys.dashboard, queryFn: getDashboard });
}

/** Picks the horizon out of the dashboard. Module-level, so the query keeps a stable `select`. */
function selectHorizon(dashboard: DashboardResponse): Horizon {
  return dashboard.horizon;
}

/**
 * Query options of the dashboard's horizon alone, for screens that only need the server's "today"
 * (`horizon.from`) and the horizon's last day. It shares the dashboard's cache entry, so it costs
 * no extra request when the dashboard is loaded.
 */
export function horizonQuery() {
  return queryOptions({ queryKey: queryKeys.dashboard, queryFn: getDashboard, select: selectHorizon });
}
