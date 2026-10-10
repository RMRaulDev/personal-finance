import { StyleSheet } from 'react-native';

import { borderWidth, control, radius, space, type Theme } from '../../../design-system';

/**
 * Styles of `Toast`: the floating box and its text. Its distance from the bottom depends on the
 * `bottom` prop and the safe area, so the component adds it.
 */
export function createToastStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    toast: {
      position: 'absolute',
      left: space[4],
      right: space[4],
      flexDirection: 'row',
      alignItems: 'center',
      gap: space[3],
      minHeight: control.lg,
      paddingVertical: space[2],
      paddingRight: space[2],
      paddingLeft: space[4],
      borderRadius: radius.lg,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.default,
      backgroundColor: colors.surface.overlay,
      // It floats over the content, so it is one of the few elements allowed a shadow.
      boxShadow: theme.shadows.md,
    },
    text: { flex: 1 },
  });
}
