import { View } from 'react-native';

import { Text, useThemedStyles } from '../../../design-system';

import type { OperationDisplay } from './operationDisplay';
import { createOperationRowStyles } from './OperationRow.styles';

/** Props of `OperationRow`. */
export interface OperationRowProps {
  /** The row texts, from `operationDisplay`. */
  operation: OperationDisplay;
}

/** One operation: signed glyph in an avatar, label, day and account, and the signed amount. */
export function OperationRow({ operation }: OperationRowProps) {
  const styles = useThemedStyles(createOperationRowStyles);

  return (
    <View style={styles.row}>
      <View style={styles.avatar} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
        <Text weight={500} color={operation.tone} style={styles.glyph}>
          {operation.glyph}
        </Text>
      </View>
      <View style={styles.texts}>
        <Text numberOfLines={1}>{operation.label}</Text>
        <Text variant="body-sm" color="secondary">
          {operation.meta}
        </Text>
      </View>
      <Text tabular weight={500} color={operation.tone}>
        {operation.amount}
      </Text>
    </View>
  );
}
