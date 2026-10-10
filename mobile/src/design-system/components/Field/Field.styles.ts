import { StyleSheet } from 'react-native';

import { focusRing, type Theme } from '../../theme';
import { borderWidth, control, fontFamily, radius, space, textStyles } from '../../tokens';

/** Field width cap of `r-field` in the web version, unless `fullWidth` lifts it. */
const FIELD_MAX_WIDTH = 320;

/**
 * Styles of `Field`: the column (and its width cap), the label, and the input with one entry per
 * size (`md`, `lg`) and its invalid and focused states.
 */
export function createFieldStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    field: { flexDirection: 'column', gap: space[1] },
    capped: { maxWidth: FIELD_MAX_WIDTH },
    label: { fontFamily: fontFamily.sans[500] },
    input: {
      paddingHorizontal: space[3],
      paddingVertical: 0,
      borderRadius: radius.md,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.control,
      backgroundColor: colors.surface.raised,
      color: colors.text.primary,
      fontFamily: fontFamily.sans[400],
      // Never below 16px: iOS zooms in on inputs smaller than 16px.
      fontSize: textStyles.body.size,
    },
    md: { height: control.md },
    lg: { height: control.lg },
    invalid: { borderColor: colors.role.danger.fill },
    focused: focusRing(theme),
  });
}
