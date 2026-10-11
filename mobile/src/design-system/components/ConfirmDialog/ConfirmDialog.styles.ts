import { StyleSheet } from 'react-native';

import type { Theme } from '../../theme';
import { radius, space } from '../../tokens';

/**
 * Styles of `ConfirmDialog`: the centered container with its side margin, the backdrop, the
 * floating dialog, and the actions row.
 */
export function createConfirmDialogStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    container: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: space[6] },
    backdrop: {
      position: 'absolute',
      top: 0,
      right: 0,
      bottom: 0,
      left: 0,
      // The scrim token darkens the screen in both themes.
      backgroundColor: colors.scrim,
    },
    dialog: {
      width: '100%',
      gap: space[3],
      padding: space[6],
      borderRadius: radius.xl,
      backgroundColor: colors.surface.overlay,
      // It floats over the screen, so it is one of the few elements allowed a shadow.
      boxShadow: theme.shadows.lg,
    },
    actions: { flexDirection: 'row', gap: space[2], marginTop: space[3] },
    primarySlot: { flex: 1 },
  });
}
