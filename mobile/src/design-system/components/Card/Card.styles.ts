import { StyleSheet } from 'react-native';

import type { Theme } from '../../theme';
import { borderWidth, radius, space } from '../../tokens';

/** Width cap of `r-card` in components.css. */
const CARD_MAX_WIDTH = 360;

/** Styles of `Card`: the bordered box (and its dense padding), the body, and the actions row. */
export function createCardStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    card: {
      backgroundColor: colors.surface.raised,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.subtle,
      borderRadius: radius.lg,
      padding: space[6],
      maxWidth: CARD_MAX_WIDTH,
    },
    dense: { padding: space[4] },
    body: { marginTop: space[2] },
    actions: { marginTop: space[4], flexDirection: 'row', gap: space[2] },
  });
}
