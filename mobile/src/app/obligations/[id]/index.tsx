import { router, useLocalSearchParams } from 'expo-router';

import { ObligationDetailScreen } from '../../../features/obligations';

/**
 * Route params of the detail: the obligation, and `from: 'dashboard'` when "Revisar" opened it there.
 * A `type`, not an `interface`: `useLocalSearchParams` needs a params record (see dashboard.tsx).
 */
type ObligationDetailParams = { id: string; from?: string };

/** Obligation detail route. Back returns to where it was opened from, and so does archiving. */
export default function ObligationDetailRoute() {
  const { id, from } = useLocalSearchParams<ObligationDetailParams>();
  return (
    <ObligationDetailScreen
      obligationId={id}
      backLabel={from === 'dashboard' ? '‹ Inicio' : '‹ Compromisos'}
      onBack={() => router.back()}
      onEdit={() => router.push({ pathname: '/obligations/[id]/edit', params: { id } })}
    />
  );
}
