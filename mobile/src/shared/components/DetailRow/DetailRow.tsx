import { View } from 'react-native';

import { Text, useThemedStyles } from '../../../design-system';

import { createDetailRowStyles } from './DetailRow.styles';

/** Props of `DetailRow`. */
export interface DetailRowProps {
  label: string;
  value: string;
  /** Fixed-width digits, for an amount. Defaults to false. */
  tabular?: boolean;
}

/**
 * A label and its value on one line ("Cuenta · Nómina"): the label in secondary text on the left,
 * the value emphasized on the right. It has no padding or border of its own, so the caller places
 * it (inside a `ListGroup` row, under a divider, ...).
 */
export function DetailRow({ label, value, tabular = false }: DetailRowProps) {
  const styles = useThemedStyles(createDetailRowStyles);

  return (
    <View style={styles.row}>
      <Text variant="body-sm" color="secondary">
        {label}
      </Text>
      <Text variant="body-sm" weight={500} tabular={tabular} style={styles.value}>
        {value}
      </Text>
    </View>
  );
}
