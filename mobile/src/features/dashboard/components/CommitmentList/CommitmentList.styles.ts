import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/**
 * Styles of `CommitmentList`: the section, its title row with the total, the new-user invite, and the
 * "Ver todos" link pulled left so its text lines up with the content.
 */
export function createCommitmentListStyles() {
  return StyleSheet.create({
    list: { gap: space[3] },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline' },
    invite: { gap: space[4], alignItems: 'flex-start' },
    // A ghost button pads its label; pulling it back aligns the label with the content edge.
    viewAll: { flexDirection: 'row', marginLeft: -space[3] },
  });
}
