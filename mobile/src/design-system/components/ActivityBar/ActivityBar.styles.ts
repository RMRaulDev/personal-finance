import { StyleSheet } from 'react-native';

import type { Theme } from '../../theme';
import { radius, space } from '../../tokens';

/** Track width, from the component proposal. */
const TRACK_WIDTH = 160;
/** Bar width: 40% of the track. The component also sweeps the bar by multiples of it. */
export const BAR_WIDTH = TRACK_WIDTH * 0.4;

/** Styles of `ActivityBar`: the thin track and the accent bar that sweeps along it. */
export function createActivityBarStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    track: {
      width: TRACK_WIDTH,
      height: space[1],
      borderRadius: radius.full,
      backgroundColor: colors.border.subtle,
      overflow: 'hidden',
    },
    bar: {
      width: BAR_WIDTH,
      height: '100%',
      borderRadius: radius.full,
      backgroundColor: colors.role.accent.fill,
    },
  });
}
