import { View } from 'react-native';

import { Button, space, Text } from '../../design-system';
import { formatMoney } from '../../lib';
import { ListGroup, SectionTitle } from '../../shared';

import type { CommitmentRow } from './model';

/** Props of `CommitmentList`. */
export interface CommitmentListProps {
  rows: CommitmentRow[];
  committedCents: number;
  /** Days in the horizon, for the empty text. */
  days: number;
  /** False only when the user has no obligations at all: shows the invite instead. */
  hasObligations: boolean;
  /** True while a payment or an undo is in flight; every "Pagar" is inactive meanwhile. */
  payPending: boolean;
  onPay: (row: CommitmentRow) => void;
}

/** Props of `CommitmentItem`. */
interface CommitmentItemProps {
  row: CommitmentRow;
  payPending: boolean;
  onPay: (row: CommitmentRow) => void;
}

/** One commitment: name and date, amount, and the "Pagar" button. `ListGroup` draws the dividers. */
function CommitmentItem({ row, payPending, onPay }: CommitmentItemProps) {
  const amount = formatMoney(row.amountCents);

  return (
    <View
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        gap: space[3],
        paddingVertical: space[3],
        paddingRight: space[3],
        paddingLeft: space[4],
      }}
    >
      <View style={{ flex: 1, minWidth: 0 }}>
        <Text numberOfLines={1} weight={500}>
          {row.name}
        </Text>
        <Text variant="body-sm" color="secondary">
          {row.when}
        </Text>
      </View>
      <Text tabular weight={500}>
        {amount}
      </Text>
      <Button
        size="sm"
        inactive={row.blocked || payPending}
        accessibilityLabel={`Pagar ${row.name}, ${amount}`}
        onPress={() => onPay(row)}
      >
        Pagar
      </Button>
    </View>
  );
}

/** "Próximos compromisos": total, rows, and the two empty states of the design. */
export function CommitmentList({ rows, committedCents, days, hasObligations, payPending, onPay }: CommitmentListProps) {
  return (
    <View style={{ gap: space[3] }}>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline' }}>
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
