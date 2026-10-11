import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/** Styles of `UpcomingItem`: the padded row, its date and amount line, and its state and actions line. */
export function createUpcomingItemStyles() {
  return StyleSheet.create({
    item: { gap: space[2], paddingVertical: space[3], paddingRight: space[3], paddingLeft: space[4] },
    top: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline', gap: space[3] },
    bottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: space[2] },
    state: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: space[1], flexShrink: 1 },
    actions: { flexDirection: 'row', gap: space[1] },
  });
}
