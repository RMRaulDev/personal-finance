import { router } from 'expo-router';

import { ObligationFormScreen } from '../../features/obligations';

/**
 * New obligation route ("Nuevo compromiso"), from the dashboard or the list. Saving returns there,
 * where the toast is shown.
 */
export default function NewObligationRoute() {
  return (
    <ObligationFormScreen
      onCancel={() => router.back()}
      onDone={() => router.back()}
      onResolveOverdue={() => router.back()}
    />
  );
}
