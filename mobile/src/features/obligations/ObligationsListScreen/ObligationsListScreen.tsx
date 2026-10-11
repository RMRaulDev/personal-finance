import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { View } from 'react-native';

import { accountsQuery, categoriesQuery, horizonQuery, obligationsQuery } from '../../../api';
import { Button, space, Text, useThemedStyles } from '../../../design-system';
import {
  BackHeader,
  BottomBar,
  errorMessage,
  ListGroup,
  LoadState,
  Screen,
  ToastOutlet,
  useDelayedLoading,
  useRefresh,
  useToast,
} from '../../../shared';
import { ArchivedSection } from '../components/ArchivedSection';
import { ObligationRow } from '../components/ObligationRow';

import { loadFailureMessage } from '../obligations.model';

import {
  activeCaption,
  activeRows,
  archivedRows,
  archivedToggleLabel,
  LOAD_FAILURE_TITLE,
} from './ObligationsListScreen.model';
import { createObligationsListScreenStyles } from './ObligationsListScreen.styles';

/** Props of `ObligationsListScreen`. */
export interface ObligationsListScreenProps {
  /** "‹ Inicio": back to the dashboard. */
  onBack: () => void;
  /** Opens an obligation's detail. */
  onOpen: (obligationId: string) => void;
  /** Opens the form to add an obligation. */
  onAdd: () => void;
}

/**
 * "Compromisos": the active obligations ordered by their next payment (what is overdue first), each
 * with its frequency and state in words, the archived ones collapsed at the end, and "Agregar
 * compromiso" as the screen's primary action. Its loading indicator only appears after 300 ms.
 */
export function ObligationsListScreen({ onBack, onOpen, onAdd }: ObligationsListScreenProps) {
  const styles = useThemedStyles(createObligationsListScreenStyles);
  const obligations = useQuery(obligationsQuery());
  // "Today" is the server's: the dashboard's horizon, already cached by the dashboard.
  const horizon = useQuery(horizonQuery());
  // Their statuses say whether an obligation's payment is blocked.
  const accounts = useQuery(accountsQuery());
  const categories = useQuery(categoriesQuery());
  const { show } = useToast();
  const [archivedExpanded, setArchivedExpanded] = useState(false);
  const { refreshing, refresh } = useRefresh(
    [obligations.refetch, horizon.refetch, accounts.refetch, categories.refetch],
    (error) => show(errorMessage(error)),
  );
  const pending = obligations.isPending || horizon.isPending;
  const loadingVisible = useDelayedLoading(pending);

  if (pending || loadingVisible) {
    return (
      <LoadState
        backLabel="‹ Inicio"
        onBack={onBack}
        title="Compromisos"
        loading={loadingVisible}
        loadingText="Cargando compromisos…"
      />
    );
  }

  // Only without data: a failed refetch keeps the last list on screen and shows a toast.
  if (obligations.data === undefined || horizon.data === undefined) {
    const error = obligations.error ?? horizon.error;
    return (
      <LoadState
        backLabel="‹ Inicio"
        onBack={onBack}
        title="Compromisos"
        loading={false}
        loadingText="Cargando compromisos…"
        failure={{
          title: LOAD_FAILURE_TITLE,
          message: loadFailureMessage(error),
          onRetry: () => {
            void obligations.refetch();
            void horizon.refetch();
          },
        }}
      />
    );
  }

  const today = horizon.data.from;
  const rows = activeRows(obligations.data, accounts.data, categories.data, today);
  const archived = archivedRows(obligations.data, today);

  return (
    <Screen
      header={<BackHeader label="‹ Inicio" onPress={onBack} />}
      refreshing={refreshing}
      onRefresh={refresh}
      // 20px above the footer, as the design leaves between the bar and the toast.
      overlay={<ToastOutlet bottom={space[5]} />}
      footer={
        <BottomBar>
          <View style={styles.primarySlot}>
            <Button variant="primary" size="lg" fullWidth onPress={onAdd}>
              Agregar compromiso
            </Button>
          </View>
        </BottomBar>
      }
    >
      <View style={styles.content}>
        <Text variant="title-2" accessibilityRole="header">
          Compromisos
        </Text>

        {rows.length > 0 ? (
          <View style={styles.active}>
            <Text variant="body-sm" color="secondary">
              {activeCaption(rows.length)}
            </Text>
            <ListGroup>
              {rows.map((row) => (
                <ObligationRow key={row.id} row={row} onPress={() => onOpen(row.id)} />
              ))}
            </ListGroup>
          </View>
        ) : (
          <View style={styles.empty}>
            <Text variant="heading" accessibilityRole="header">
              Aún no tienes compromisos
            </Text>
            <Text color="secondary">
              Agrega lo que pagas con regularidad, como la renta, el internet o una suscripción. Así «Disponible para
              gastar» te dice cuánto tienes libre de verdad.
            </Text>
          </View>
        )}

        {archived.length > 0 && (
          <ArchivedSection
            rows={archived}
            toggleLabel={archivedToggleLabel(archived.length, archivedExpanded)}
            expanded={archivedExpanded}
            onToggle={() => setArchivedExpanded((expanded) => !expanded)}
            onOpen={onOpen}
          />
        )}
      </View>
    </Screen>
  );
}
