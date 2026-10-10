import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/** Styles of `CommitmentItem`: the row and its name and date column. */
export function createCommitmentItemStyles() {
  return StyleSheet.create({
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
