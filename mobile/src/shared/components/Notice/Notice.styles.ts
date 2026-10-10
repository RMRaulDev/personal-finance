import { StyleSheet } from 'react-native';

import { borderWidth, radius, space, type Theme } from '../../../design-system';

/** Styles of `Notice`: the info-toned box, the icon and text row, and the text column. */
export function createNoticeStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    notice: {
      gap: space[3],
      padding: space[4],
      borderRadius: radius.lg,
      borderWidth: borderWidth.hairline,
      borderColor: colors.role.info.border,
      backgroundColor: colors.role.info.bg,
    },
    content: { flexDirection: 'row', alignItems: 'flex-start', gap: space[3] },
    text: { flex: 1 },
  });
}
