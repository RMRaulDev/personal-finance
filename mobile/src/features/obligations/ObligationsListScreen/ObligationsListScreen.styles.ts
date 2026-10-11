import { StyleSheet } from 'react-native';

import { space } from '../../../design-system';

/**
 * Styles of `ObligationsListScreen`: the content column with the design's 24px gap (tighter than the
 * dashboard's sections), the caption above the rows, the empty state, and the footer's button slot.
 */
export function createObligationsListScreenStyles() {
  return StyleSheet.create({
    content: { gap: space[6] },
    active: { gap: space[2] },
    empty: { gap: space[3], paddingTop: space[6] },
    primarySlot: { flex: 1 },
  });
}
