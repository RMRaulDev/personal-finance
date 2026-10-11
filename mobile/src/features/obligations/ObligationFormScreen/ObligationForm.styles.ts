import { StyleSheet } from 'react-native';

import { borderWidth, space, type Theme } from '../../../design-system';

/**
 * Styles of `ObligationForm`: the column of fields with the design's 24px gap, the "Fin" group, the
 * single-account row between dividers, and the footer's button slot.
 */
export function createObligationFormStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    content: { gap: space[6] },
    group: { gap: space[2] },
    accountFact: {
      paddingVertical: space[3],
      borderTopWidth: borderWidth.hairline,
      borderBottomWidth: borderWidth.hairline,
      borderColor: colors.border.subtle,
    },
    primarySlot: { flex: 1 },
  });
}
