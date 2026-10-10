import { router, useLocalSearchParams } from 'expo-router';

import { DashboardScreen } from '../features/dashboard';

/**
 * Route params of the dashboard: the account the onboarding just created, if it comes from there.
 * A `type`, not an `interface`: `useLocalSearchParams` accepts a route string or a params record
 * as its generic, and an interface (no implicit index signature) satisfies neither (TS2344).
 */
type DashboardParams = { accountName?: string };

/** Dashboard route: the home screen. */
export default function DashboardRoute() {
  const { accountName } = useLocalSearchParams<DashboardParams>();
  return (
    <DashboardScreen
      arrivalAccountName={accountName}
      // Drop the param once its toast is shown, so a remount does not show it again.
      onArrivalShown={() => router.setParams({ accountName: undefined })}
    />
  );
}
