import { StyleSheet } from 'react-native';

import { borderWidth, space, type Theme } from '../../../../design-system';

/**
 * Styles of `RegistrationSheet`: the single-account row and the actions. The sheet itself is the
 * shared `BottomSheet`.
 */
export function createRegistrationSheetStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    accountFact: {
      paddingTop: space[3],
      borderTopWidth: borderWidth.hairline,
      borderTopColor: colors.border.subtle,
    },
    actions: { flexDirection: 'row', gap: space[2] },
    primarySlot: { flex: 1 },
  });
}
