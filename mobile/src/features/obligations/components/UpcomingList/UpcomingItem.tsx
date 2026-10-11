import { View } from 'react-native';

import { Badge, Button, Text, useThemedStyles } from '../../../../design-system';

import { createUpcomingItemStyles } from './UpcomingItem.styles';
import type { UpcomingRowData } from './UpcomingList';

/** Props of `UpcomingItem`. */
interface UpcomingItemProps {
  row: UpcomingRowData;
  busy: boolean;
  onPay: () => void;
  onSkip: () => void;
}

/** One occurrence to resolve: its date and amount, its state in words, and "Saltar" and "Pagar". */
export function UpcomingItem({ row, busy, onPay, onSkip }: UpcomingItemProps) {
  const styles = useThemedStyles(createUpcomingItemStyles);

  return (
    <View style={styles.item}>
      <View style={styles.top}>
        <Text weight={500}>{row.date}</Text>
        <Text tabular weight={500}>
          {row.amount}
        </Text>
      </View>
      <View style={styles.bottom}>
        <View style={styles.state}>
          {row.overdue && <Badge role="warning">Vencida</Badge>}
          <Text variant="body-sm" color="secondary">
            {row.when}
          </Text>
        </View>
        <View style={styles.actions}>
          <Button
            variant="ghost"
            size="sm"
            inactive={busy}
            accessibilityLabel={`Saltar el pago del ${row.date}`}
            onPress={onSkip}
          >
            Saltar
          </Button>
          <Button size="sm" inactive={busy} accessibilityLabel={`Pagar el ${row.date}`} onPress={onPay}>
            Pagar
          </Button>
        </View>
      </View>
    </View>
  );
}
