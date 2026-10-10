import { View } from 'react-native';

import { borderWidth, radius, space, Text, useTheme } from '../design-system';

import { operationAvatar } from './designSpec';
import type { OperationDisplay } from './components/OperationRow/operationDisplay';

/** Props of `OperationRow`. */
export interface OperationRowProps {
  /** The row texts, from `operationDisplay`. */
  operation: OperationDisplay;
}

/** One operation: signed glyph in an avatar, label, day and account, and the signed amount. */
export function OperationRow({ operation }: OperationRowProps) {
  const { colors } = useTheme();

  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: space[3], paddingVertical: space[2] }}>
      <View
        style={{
          width: operationAvatar.size,
          height: operationAvatar.size,
          borderRadius: radius.full,
          borderWidth: borderWidth.hairline,
          borderColor: colors.border.subtle,
          backgroundColor: colors.surface.raised,
          alignItems: 'center',
          justifyContent: 'center',
        }}
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants"
      >
        <Text
          weight={500}
          color={operation.tone}
          style={{
            fontSize: operationAvatar.glyphSize,
            lineHeight: operationAvatar.glyphLineHeight,
            // Android adds font padding above and below the text, which pushes the glyph off center.
            includeFontPadding: false,
            textAlignVertical: 'center',
          }}
        >
          {operation.glyph}
        </Text>
      </View>
      <View style={{ flex: 1, minWidth: 0 }}>
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
