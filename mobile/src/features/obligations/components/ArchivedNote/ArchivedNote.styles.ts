import { StyleSheet } from 'react-native';

import { borderWidth, radius, space, type Theme } from '../../../../design-system';

/**
 * Archive icon from the "Compromisos v1" design (Claude Design), outside the design system's size
 * scale: 20px with a 1.5px stroke, nudged 2px down to align with the text.
 */
export const ARCHIVE_ICON = { size: 20, strokeWidth: 1.5, offsetTop: 2 } as const;

/** Styles of `ArchivedNote`: the bordered card with the icon and text row, and the icon's nudge. */
export function createArchivedNoteStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    note: {
      flexDirection: 'row',
      alignItems: 'flex-start',
      gap: space[3],
      padding: space[4],
      borderRadius: radius.lg,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.subtle,
      backgroundColor: colors.surface.raised,
    },
    icon: { marginTop: ARCHIVE_ICON.offsetTop },
    text: { flex: 1 },
  });
}
