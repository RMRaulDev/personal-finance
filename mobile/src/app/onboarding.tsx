import { router } from 'expo-router';

import { OnboardingScreen } from '../features/onboarding';

/** Onboarding route: once the account exists, replaces itself with the dashboard. */
export default function OnboardingRoute() {
  return (
    <OnboardingScreen
      // `replace`, so Back from the dashboard does not return to the onboarding. The name travels as
      // a route param for the dashboard's one-time "{nombre} está lista." toast.
      onDone={(accountName) => router.replace({ pathname: '/dashboard', params: { accountName } })}
    />
  );
}
