import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/**
 * Styles of `PaySheet`: the title block, the compact view with its "Cambiar…" link pulled left so its
 * text lines up, the date choices with their help, and the actions. The sheet itself is the shared
 * `BottomSheet`.
 */
export function createPaySheetStyles() {
  return StyleSheet.create({
    titleBlock: { gap: space[1] },
    compact: { gap: space[2] },
    // A ghost button pads its label; pulling it back aligns the label with the content edge.
    expandLink: { flexDirection: 'row', marginLeft: -space[3] },
    dates: { gap: space[2] },
    actions: { flexDirection: 'row', gap: space[2] },
    primarySlot: { flex: 1 },
  });
}
