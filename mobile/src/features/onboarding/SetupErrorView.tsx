import { ScrollView, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { isApiError } from '../../api';
import { Button, space, Text } from '../../design-system';
import { formatMoney } from '../../lib';
import { DetailRow, errorMessage, ListGroup } from '../../shared';

/** Props of `SetupErrorView`. */
export interface SetupErrorViewProps {
  /** Why the setup failed; a network failure gets the design's "No pudimos conectar" text. */
  error: unknown;
  /** The trimmed account name that was sent. */
  accountName: string;
  /** The opening balance that was sent, in cents. */
  openingBalanceCents: number;
  /** True while a retry runs; both buttons are inactive meanwhile. */
  pending: boolean;
  /** Sends the setup again with the same values. */
  onRetry: () => void;
  /** Goes back to step 2 with everything the user typed. */
  onEdit: () => void;
}

/** Props of `SummaryLine`. */
interface SummaryLineProps {
  label: string;
  value: string;
  /** Fixed-width digits, for the amount. */
  tabular?: boolean;
}

/** One padded row of the summary: what was sent, as a shared `DetailRow`. */
function SummaryLine({ label, value, tabular }: SummaryLineProps) {
  return (
    <View style={{ paddingVertical: space[3], paddingHorizontal: space[4] }}>
      <DetailRow label={label} value={value} tabular={tabular} />
    </View>
  );
}

/**
 * The setup failed: what happened, a summary of what will be sent again, "Reintentar", and
 * "Editar datos". A network failure reads "No pudimos conectar"; any other failure reads "Algo salió
 * mal" with the shared error copy.
 */
export function SetupErrorView({
  error,
  accountName,
  openingBalanceCents,
  pending,
  onRetry,
  onEdit,
}: SetupErrorViewProps) {
  const insets = useSafeAreaInsets();
  const network = isApiError(error) && error.kind === 'network';

  return (
    <>
      <ScrollView
        style={{ flex: 1 }}
        contentContainerStyle={{ paddingTop: space[16], paddingHorizontal: space[5], gap: space[6] }}
      >
        <View accessibilityRole="alert" style={{ gap: space[3] }}>
          <Text variant="title-2" accessibilityRole="header">
            {network ? 'No pudimos conectar' : 'Algo salió mal'}
          </Text>
          <Text color="secondary">
            {network ? 'Revisa tu conexión y vuelve a intentarlo. Lo que escribiste sigue aquí.' : errorMessage(error)}
          </Text>
        </View>
        <ListGroup>
          <SummaryLine label="Cuenta" value={accountName} />
          <SummaryLine label="Saldo de hoy" value={formatMoney(openingBalanceCents)} tabular />
        </ListGroup>
      </ScrollView>
      <View
        style={{
          paddingTop: space[4],
          paddingHorizontal: space[5],
          paddingBottom: space[10] + insets.bottom,
          gap: space[2],
        }}
      >
        <Button variant="primary" size="lg" fullWidth inactive={pending} onPress={onRetry}>
          Reintentar
        </Button>
        <Button variant="ghost" size="lg" fullWidth inactive={pending} onPress={onEdit}>
          Editar datos
        </Button>
      </View>
    </>
  );
}
