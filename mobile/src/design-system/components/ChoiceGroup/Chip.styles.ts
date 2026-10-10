import { StyleSheet } from 'react-native';

import { focusRing, type Theme } from '../../theme';
import { borderWidth, control, radius, space } from '../../tokens';

/** Styles of `Chip`: the raised, outlined pill and its pressed, selected, and focused states. */
export function createChipStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    chip: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: space[1],
      height: control.touch,
      paddingHorizontal: space[4],
      borderRadius: radius.full,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.control,
      backgroundColor: colors.surface.raised,
    },
    // While pressed it takes the web version's hover color, like `Button`.
    pressed: { backgroundColor: colors.surface.hover },
    // Listed after `pressed`: a selected chip keeps its selected background while pressed.
    selected: { borderColor: colors.text.primary, backgroundColor: colors.surface.selected },
    focused: focusRing(theme),
  });
}
