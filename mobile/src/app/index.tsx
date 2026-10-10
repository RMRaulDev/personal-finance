import { Redirect } from 'expo-router';

import { EntryGate } from '../features/onboarding';

/** Entry route: opens the onboarding when the user has no account yet, the dashboard otherwise. */
export default function EntryRoute() {
  // `Redirect` replaces this route, so Back never returns to the gate.
  return <EntryGate onboarding={<Redirect href="/onboarding" />} home={<Redirect href="/dashboard" />} />;
}
