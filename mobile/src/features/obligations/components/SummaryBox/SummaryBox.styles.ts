import { StyleSheet } from 'react-native';

import { borderWidth, radius, space, type Theme } from '../../../../design-system';

/** Styles of `SummaryBox`: the outlined box (no background, a default border) and its two lines. */
export function createSummaryBoxStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    box: {
      gap: space[1],
      padding: space[4],
      borderRadius: radius.lg,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.default,
    },
  });
}
