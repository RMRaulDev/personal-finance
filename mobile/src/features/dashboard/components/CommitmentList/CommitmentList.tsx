import { View } from 'react-native';

import { Text, useThemedStyles } from '../../../../design-system';
import { formatMoney } from '../../../../lib';
import { ListGroup, SectionTitle } from '../../../../shared';
import type { CommitmentRow } from '../../dashboard.model';

import { CommitmentItem } from './CommitmentItem';
import { createCommitmentListStyles } from './CommitmentList.styles';

/** Props of `CommitmentList`. */
export interface CommitmentListProps {
  rows: CommitmentRow[];
  committedCents: number;
  /** Days in the horizon, for the empty text. */
  days: number;
  /** False only when the user has no obligations at all: shows the invite instead. */
  hasObligations: boolean;
  /** True while a payment, undo, or registration runs; every "Pagar" is inactive meanwhile. */
  payPending: boolean;
  onPay: (row: CommitmentRow) => void;
}

/** "Próximos compromisos": total, rows, and the two empty states of the design. */
export function CommitmentList({ rows, committedCents, days, hasObligations, payPending, onPay }: CommitmentListProps) {
  const styles = useThemedStyles(createCommitmentListStyles);

  return (
    <View style={styles.list}>
      <View style={styles.header}>
        <SectionTitle>Próximos compromisos</SectionTitle>
        <SectionTitle tabular accessibilityRole="text">
          {formatMoney(committedCents)}
        </SectionTitle>
      </View>

      {rows.length > 0 && (
        <ListGroup>
          {rows.map((row) => (
            <CommitmentItem key={row.key} row={row} payPending={payPending} onPay={onPay} />
          ))}
        </ListGroup>
      )}

      {rows.length === 0 && !hasObligations && (
        // New user: explain what commitments are for. "Agregar compromiso" waits for its design.
        <ListGroup padded>
          <Text color="secondary">Agrega tu renta o suscripciones para ver cuánto tienes realmente libre.</Text>
        </ListGroup>
      )}

      {rows.length === 0 && hasObligations && (
        <Text variant="body-sm" color="secondary">
          {`Nada pendiente en los próximos ${days} días.`}
        </Text>
      )}
    </View>
  );
}
