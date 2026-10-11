import { View } from 'react-native';

import { Badge, PressableRow, Text, useThemedStyles } from '../../../../design-system';

import { createObligationRowStyles } from './ObligationRow.styles';

/** What an active obligation row shows (`activeRows` maps it). */
export interface ObligationRowData {
  name: string;
  amount: string;
  frequency: string;
  badge: { text: string; role: 'warning' | 'info' } | null;
  status: string;
  accessibilityLabel: string;
}

/** Props of `ObligationRow`. */
export interface ObligationRowProps {
  row: ObligationRowData;
  /** Opens the obligation's detail. */
  onPress: () => void;
}

/**
 * One active obligation of the list: name, frequency in words, its state with text (and a badge when
 * something is overdue or blocked), and the amount. The whole row opens the detail.
 */
export function ObligationRow({ row, onPress }: ObligationRowProps) {
  const styles = useThemedStyles(createObligationRowStyles);

  return (
    <PressableRow
      accessibilityLabel={row.accessibilityLabel}
      onPress={onPress}
      trailing={
        <Text tabular weight={500} style={styles.amount}>
          {row.amount}
        </Text>
      }
    >
      <View style={styles.texts}>
        <Text weight={500} numberOfLines={1}>
          {row.name}
        </Text>
        <Text variant="body-sm" color="secondary">
          {row.frequency}
        </Text>
        <View style={styles.statusLine}>
          {row.badge && <Badge role={row.badge.role}>{row.badge.text}</Badge>}
          {row.status !== '' && (
            <Text variant="body-sm" color="secondary">
              {row.status}
            </Text>
          )}
        </View>
      </View>
    </PressableRow>
  );
}
