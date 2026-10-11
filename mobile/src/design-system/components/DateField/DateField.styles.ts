import { StyleSheet } from 'react-native';

import { focusRing, type Theme } from '../../theme';
import { borderWidth, control, radius, space } from '../../tokens';

/**
 * Styles of `DateField`: the column with its label and messages, and the 48px box with the input's
 * borders and its pressed, invalid, and focused states.
 */
export function createDateFieldStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    field: { flexDirection: 'column', gap: space[2] },
    box: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: space[3],
      height: control.lg,
      paddingHorizontal: space[4],
      borderRadius: radius.md,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.control,
      backgroundColor: colors.surface.raised,
    },
    // While pressed it takes the web version's hover color, like `Button`.
    pressed: { backgroundColor: colors.surface.hover },
    invalid: { borderColor: colors.role.danger.fill },
    focused: focusRing(theme),
    text: { flexShrink: 1 },
  });
}
