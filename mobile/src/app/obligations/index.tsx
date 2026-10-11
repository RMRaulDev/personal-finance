import { router } from 'expo-router';

import { ObligationsListScreen } from '../../features/obligations';

/** Obligations list route ("Compromisos"), opened from the dashboard. */
export default function ObligationsListRoute() {
  return (
    <ObligationsListScreen
      onBack={() => router.back()}
      onOpen={(id) => router.push({ pathname: '/obligations/[id]', params: { id } })}
      onAdd={() => router.push('/obligations/new')}
    />
  );
}
