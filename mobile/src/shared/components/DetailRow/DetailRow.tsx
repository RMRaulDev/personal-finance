import { View } from 'react-native';

import { space, Text } from '../../../design-system';

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
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: space[3] }}>
      <Text variant="body-sm" color="secondary">
        {label}
      </Text>
      <Text variant="body-sm" weight={500} tabular={tabular} style={{ flexShrink: 1, textAlign: 'right' }}>
        {value}
      </Text>
    </View>
  );
}
