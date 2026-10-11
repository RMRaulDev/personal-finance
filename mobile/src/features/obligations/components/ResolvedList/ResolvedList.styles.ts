import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/** Styles of `ResolvedList`: the section, and each row with its icon, texts, and action. */
export function createResolvedListStyles() {
  return StyleSheet.create({
    section: { gap: space[3] },
    item: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: space[3],
      paddingVertical: space[3],
      paddingRight: space[3],
      paddingLeft: space[4],
    },
    texts: { flex: 1, minWidth: 0 },
  });
}
