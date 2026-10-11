import { StyleSheet } from 'react-native';

import { borderWidth, radius, space, type Theme } from '../../../../design-system';

/**
 * Styles of `ArchivedSection`: the section, the toggle pulled left so its text lines up with the
 * content, the borderless-background group of rows, its dividers, and the row texts.
 */
export function createArchivedSectionStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    section: { gap: space[2] },
    // A ghost button pads its label; pulling it back aligns the label with the content edge.
    toggle: { flexDirection: 'row', marginLeft: -space[3] },
    // Archived rows sit on the page, not on a raised surface: only the border delimits them.
    group: {
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.subtle,
      borderRadius: radius.lg,
      overflow: 'hidden',
    },
    divider: { height: borderWidth.hairline, backgroundColor: colors.border.subtle },
    texts: { gap: space[0.5] },
  });
}
