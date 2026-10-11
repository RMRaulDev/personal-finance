import { StyleSheet } from 'react-native';

import { borderWidth, radius, type Theme } from '../../../../design-system';

/**
 * Resolution icon from the "Compromisos v1" design (Claude Design), outside the design system's
 * size scale: a 28px bordered circle around a 16px glyph.
 */
export const RESOLVED_ICON = { circle: 28, glyph: 16, checkStroke: 1.75, skipStroke: 1.5 } as const;

/** Styles of `ResolvedIcon`: the bordered circle that centers the glyph. */
export function createResolvedIconStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    circle: {
      width: RESOLVED_ICON.circle,
      height: RESOLVED_ICON.circle,
      alignItems: 'center',
      justifyContent: 'center',
      borderRadius: radius.full,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.subtle,
    },
  });
}
