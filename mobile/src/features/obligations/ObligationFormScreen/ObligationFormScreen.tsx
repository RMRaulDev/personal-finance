import { useQuery } from '@tanstack/react-query';

import {
  accountsQuery,
  categoriesQuery,
  horizonQuery,
  obligationOccurrencesQuery,
  obligationQuery,
  obligationsQuery,
} from '../../../api';
import { LoadState, useDelayedLoading } from '../../../shared';
import { loadFailureMessage } from '../obligations.model';

import { ObligationForm } from './ObligationForm';

/** Props of `ObligationFormScreen`. */
export interface ObligationFormScreenProps {
  /** The obligation to edit; undefined to create one. */
  obligationId?: string;
  /** "Cancelar": back without saving. */
  onCancel: () => void;
  /** Back after saving; the toast is already shown. */
  onDone: () => void;
  /** "Resolver vencidos": back to the detail. */
  onResolveOverdue: () => void;
}

/**
 * "Nuevo compromiso" / "Editar {nombre}": loads what the form needs (the server's today, the
 * accounts, the categories, and, to edit, the obligation), then shows `ObligationForm` with it.
 */
export function ObligationFormScreen({ obligationId, onCancel, onDone, onResolveOverdue }: ObligationFormScreenProps) {
  const editing = obligationId !== undefined;
  const horizon = useQuery(horizonQuery());
  const accounts = useQuery(accountsQuery());
  const categories = useQuery(categoriesQuery());
  // Only to catch a repeated name before saving; the server checks it anyway.
  const obligations = useQuery(obligationsQuery());
  const obligation = useQuery({ ...obligationQuery(obligationId ?? ''), enabled: editing });
  // Only to name an advance payment in a calendar conflict.
  const occurrences = useQuery({ ...obligationOccurrencesQuery(obligationId ?? ''), enabled: editing });
  const pending = horizon.isPending || accounts.isPending || categories.isPending || (editing && obligation.isPending);
  const loadingVisible = useDelayedLoading(pending);

  if (pending || loadingVisible) {
    return <LoadState backLabel="Cancelar" onBack={onCancel} loading={loadingVisible} loadingText="Cargando…" />;
  }

  if (
    horizon.data === undefined ||
    accounts.data === undefined ||
    categories.data === undefined ||
    (editing && obligation.data === undefined)
  ) {
    return (
      <LoadState
        backLabel="Cancelar"
        onBack={onCancel}
        loading={false}
        loadingText="Cargando…"
        failure={{
          title: 'No pudimos abrir el formulario',
          message: loadFailureMessage(horizon.error ?? accounts.error ?? categories.error ?? obligation.error),
          onRetry: () => {
            void horizon.refetch();
            void accounts.refetch();
            void categories.refetch();
            if (editing) {
              void obligation.refetch();
            }
          },
        }}
      />
    );
  }

  return (
    <ObligationForm
      editing={obligation.data}
      occurrences={occurrences.data}
      obligations={obligations.data}
      accounts={accounts.data}
      categories={categories.data}
      horizon={horizon.data}
      onCancel={onCancel}
      onDone={onDone}
      onResolveOverdue={onResolveOverdue}
    />
  );
}
