import { request } from '../../client';

import type { DashboardResponse } from './types';

// Endpoints of the dashboard resource: `GET /api/v1/dashboard` (`entry.web.dashboard`).

/** `GET /dashboard`: everything the home screen shows, computed by the server. */
export function getDashboard(): Promise<DashboardResponse> {
  return request<DashboardResponse>('GET', '/dashboard');
}
