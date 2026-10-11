import { router, useLocalSearchParams } from 'expo-router';

import { ObligationFormScreen } from '../../../features/obligations';

/** Route params of the edit form: the obligation to edit. A `type` (see dashboard.tsx). */
type EditObligationParams = { id: string };

/**
 * Edit obligation route ("Editar {nombre}"), opened from its detail. Saving and "Resolver vencidos"
 * return to the detail.
 */
export default function EditObligationRoute() {
  const { id } = useLocalSearchParams<EditObligationParams>();
  return (
    <ObligationFormScreen
      obligationId={id}
      onCancel={() => router.back()}
      onDone={() => router.back()}
      onResolveOverdue={() => router.back()}
    />
  );
}
