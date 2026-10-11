import { StyleSheet } from 'react-native';

import { focusRing, type Theme } from '../../theme';
import { borderWidth, control, radius } from '../../tokens';

/**
 * Styles of `DayCell`: the round 44px cell and its today, selected, pressed, and focused states,
 * with the matching label colors.
 */
export function createDayCellStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    slot: { flex: 1, alignItems: 'center' },
    cell: {
      width: control.touch,
      height: control.touch,
      alignItems: 'center',
      justifyContent: 'center',
      borderRadius: radius.full,
      borderWidth: borderWidth.hairline,
      borderColor: 'transparent',
    },
    // Today is marked by a border and weight 600, never by color alone.
    today: { borderColor: colors.border.default },
    pressed: { backgroundColor: colors.surface.hover },
    selected: { backgroundColor: colors.role.accent.fill, borderColor: colors.role.accent.fill },
    focused: focusRing(theme),
    label: { color: colors.text.primary },
    selectedLabel: { color: colors.role.accent.on },
    disabledLabel: { color: colors.text.disabled },
  });
}
