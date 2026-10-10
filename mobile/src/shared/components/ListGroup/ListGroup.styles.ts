import { StyleSheet } from 'react-native';

import { borderWidth, radius, space, type Theme } from '../../../design-system';

/** Styles of `ListGroup`: the bordered surface, its padded variant, and the divider between rows. */
export function createListGroupStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    group: {
      backgroundColor: colors.surface.raised,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.subtle,
      borderRadius: radius.lg,
      overflow: 'hidden',
    },
    padded: { padding: space[5] },
    divider: { height: borderWidth.hairline, backgroundColor: colors.border.subtle },
  });
}
