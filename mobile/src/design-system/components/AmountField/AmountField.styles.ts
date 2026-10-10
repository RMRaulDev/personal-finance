import { StyleSheet } from 'react-native';

import { focusRing, type Theme } from '../../theme';
import { borderWidth, control, fontFamily, radius, space, textStyles } from '../../tokens';

/** Styles of `AmountField`: the column, the 64px box with its states, and the amount input. */
export function createAmountFieldStyles(theme: Theme) {
  const { colors } = theme;
  const amount = textStyles.amount;
  return StyleSheet.create({
    field: { flexDirection: 'column', gap: space[1] },
    box: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: space[2],
      height: control.xl,
      paddingHorizontal: space[4],
      borderRadius: radius.md,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.control,
      backgroundColor: colors.surface.raised,
    },
    boxInvalid: { borderColor: colors.role.danger.fill },
    // The ring goes on the box (`:focus-within` in the web version).
    boxFocused: focusRing(theme),
    // A focused invalid box keeps its ring, in danger.
    boxFocusedInvalid: { outlineColor: colors.role.danger.fill },
    input: {
      flex: 1,
      minWidth: 0,
      height: '100%',
      padding: 0,
      fontFamily: fontFamily.sans[amount.weight],
      fontSize: amount.size,
      letterSpacing: amount.tracking * amount.size,
      fontVariant: ['tabular-nums'],
      color: colors.text.primary,
      // Android pads text vertically by default, which pushes a large single line off center.
      includeFontPadding: false,
      textAlignVertical: 'center',
    },
  });
}
