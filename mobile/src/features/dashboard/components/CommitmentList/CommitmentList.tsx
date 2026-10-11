import { View } from 'react-native';

import { Button, Text, useThemedStyles } from '../../../../design-system';
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
  /**
   * Active obligations, for "Ver todos los compromisos (n)". Undefined hides the link: until the
   * obligations load, or when the user has none (active or archived).
   */
  activeCount: number | undefined;
  /** True while a payment, undo, or registration runs; every "Pagar" is inactive meanwhile. */
  payPending: boolean;
  onPay: (row: CommitmentRow) => void;
  /** Opens the detail of a row whose payment is blocked ("Revisar"). */
  onReview: (row: CommitmentRow) => void;
  /** Opens the obligations list. */
  onViewAll: () => void;
  /** Opens the form to add an obligation, from the new-user invite. */
  onAddObligation: () => void;
}

/**
 * "Próximos compromisos": total, rows, the two empty states of the design (the new-user invite with
 * "Agregar compromiso", or nothing due), and "Ver todos los compromisos (n)" under them.
 */
export function CommitmentList({
  rows,
  committedCents,
  days,
  hasObligations,
  activeCount,
  payPending,
  onPay,
  onReview,
  onViewAll,
  onAddObligation,
}: CommitmentListProps) {
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
            <CommitmentItem key={row.key} row={row} payPending={payPending} onPay={onPay} onReview={onReview} />
          ))}
        </ListGroup>
      )}

      {rows.length === 0 && !hasObligations && (
        // New user: explain what commitments are for, and offer to add the first one.
        <ListGroup padded>
          <View style={styles.invite}>
            <Text color="secondary">Agrega tu renta o suscripciones para ver cuánto tienes realmente libre.</Text>
            <Button size="sm" onPress={onAddObligation}>
              Agregar compromiso
            </Button>
          </View>
        </ListGroup>
      )}

      {rows.length === 0 && hasObligations && (
        <Text variant="body-sm" color="secondary">
          {`Nada pendiente en los próximos ${days} días.`}
        </Text>
      )}

      {activeCount !== undefined && (
        // A ghost button: it must not compete with "+ Gasto", the screen's primary action.
        <View style={styles.viewAll}>
          <Button variant="ghost" size="sm" onPress={onViewAll}>
            {`Ver todos los compromisos (${activeCount})`}
          </Button>
        </View>
      )}
    </View>
  );
}
