import { StyleSheet } from 'react-native';

import { borderWidth, radius, space, type Theme } from '../../../design-system';

/** Tone of a notice: the design system role whose background, border, and icon color it takes. */
export type NoticeTone = 'info' | 'warning' | 'danger';

/**
 * Styles of `Notice`: the box with one entry per tone (`info`, `warning`, `danger`), the icon and
 * text row, and the text column.
 */
export function createNoticeStyles(theme: Theme) {
  const { colors } = theme;
  /** Background and border of a tone. */
  const tone = (role: NoticeTone) => ({ borderColor: colors.role[role].border, backgroundColor: colors.role[role].bg });
  return StyleSheet.create({
    notice: {
      gap: space[3],
      padding: space[4],
      borderRadius: radius.lg,
      borderWidth: borderWidth.hairline,
    },
    info: tone('info'),
    warning: tone('warning'),
    danger: tone('danger'),
    content: { flexDirection: 'row', alignItems: 'flex-start', gap: space[3] },
    texts: { flex: 1, gap: space[1] },
  });
}
