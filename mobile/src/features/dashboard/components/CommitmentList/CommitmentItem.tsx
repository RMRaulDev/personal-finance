import { View } from 'react-native';

import { Button, Text, useThemedStyles } from '../../../../design-system';
import { formatMoney } from '../../../../lib';
import type { CommitmentRow } from '../../dashboard.model';

import { createCommitmentItemStyles } from './CommitmentItem.styles';

/** Props of `CommitmentItem`. */
interface CommitmentItemProps {
  row: CommitmentRow;
  payPending: boolean;
  onPay: (row: CommitmentRow) => void;
}

/** One commitment: name and date, amount, and the "Pagar" button. `ListGroup` draws the dividers. */
export function CommitmentItem({ row, payPending, onPay }: CommitmentItemProps) {
  const styles = useThemedStyles(createCommitmentItemStyles);
  const amount = formatMoney(row.amountCents);

  return (
    <View style={styles.item}>
      <View style={styles.texts}>
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
