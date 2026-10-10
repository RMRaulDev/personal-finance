import { StyleSheet } from 'react-native';

import { space } from '../../../design-system';

/**
 * Styles of `Screen`: the area between header and footer, and the scrolling content with its
 * section gap. The content's bottom padding depends on the footer and the safe area, so the
 * component adds it.
 */
export function createScreenStyles() {
  return StyleSheet.create({
    body: { flex: 1 },
    content: {
      gap: space[8],
      paddingTop: space[4],
      paddingHorizontal: space[5],
    },
    // Above a footer the content ends where the footer starts, which already clears the bottom
    // safe area, so only a gap above the footer is left.
    contentAboveFooter: { paddingBottom: space[10] },
  });
}
