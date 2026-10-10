import { StyleSheet } from 'react-native';

import { borderWidth, space, type Theme } from '../../../../design-system';

/**
 * Styles of `ActionBar`: the raised bar with its top border, and the slot that gives the primary
 * action the rest of the row. Its bottom padding includes the safe area, so the component adds it.
 */
export function createActionBarStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    bar: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: space[2],
      paddingTop: space[3],
      paddingHorizontal: space[4],
      backgroundColor: colors.surface.raised,
      borderTopWidth: borderWidth.hairline,
      borderTopColor: colors.border.subtle,
    },
    primarySlot: { flex: 1 },
  });
}
