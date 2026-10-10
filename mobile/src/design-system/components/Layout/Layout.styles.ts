import { StyleSheet } from 'react-native';

import type { Theme } from '../../theme';
import { space } from '../../tokens';

/** Styles of the layout primitives: `Root`'s page background, `Stack`'s column, and `Row`'s row. */
export function createLayoutStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    root: { flex: 1, backgroundColor: colors.surface.page },
    stack: { flexDirection: 'column', gap: space[4] },
    row: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: space[3] },
  });
}
