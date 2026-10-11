import { StyleSheet } from 'react-native';

import { focusRing, type Theme } from '../../theme';
import { borderWidth, space } from '../../tokens';

/** Minimum row height from the "Fila clicable" proposal ("Compromisos v1" design): 64px. */
const ROW_MIN_HEIGHT = 64;

/** Chevron from the proposal: 16px with a 1.6px stroke. */
export const CHEVRON = { size: 16, strokeWidth: 1.6 } as const;

/**
 * Styles of `PressableRow`: the row with its padding, the pressed background, the focus ring drawn
 * inside the row (a list group clips anything outside it), and the content column.
 */
export function createPressableRowStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    row: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: space[3],
      minHeight: ROW_MIN_HEIGHT,
      paddingVertical: space[3],
      paddingRight: space[3],
      paddingLeft: space[4],
    },
    // While pressed it takes the proposal's "presionada" background.
    pressed: { backgroundColor: colors.surface.selected },
    // The ring goes 2px inward, so the rounded, clipped list group does not cut it.
    focused: { ...focusRing(theme), outlineOffset: -borderWidth.focus },
    content: { flex: 1, minWidth: 0 },
  });
}
