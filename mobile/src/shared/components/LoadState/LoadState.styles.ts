import { StyleSheet } from 'react-native';

import { space } from '../../../design-system';

/**
 * Bottom padding of the loading view, from the "Compromisos v1" design (Claude Design), outside the
 * design system's spacing scale: it lifts the centered indicator above the middle of the screen.
 */
const LOADING_BOTTOM_PADDING = 160;

/**
 * Styles of `LoadState`: the title, the centered loading column, the error texts, and the footer
 * with "Reintentar". The footer's bottom padding includes the safe area, so the component adds it.
 */
export function createLoadStateStyles() {
  return StyleSheet.create({
    title: { paddingTop: space[2], paddingHorizontal: space[5] },
    loading: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      gap: space[4],
      paddingBottom: LOADING_BOTTOM_PADDING,
    },
    error: { flex: 1, gap: space[3], paddingTop: space[6], paddingHorizontal: space[5] },
    footer: { paddingTop: space[4], paddingHorizontal: space[5] },
  });
}
