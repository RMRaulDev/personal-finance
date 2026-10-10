import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/** Styles of `CommitmentList`: the section and its title row with the total. */
export function createCommitmentListStyles() {
  return StyleSheet.create({
    list: { gap: space[3] },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline' },
  });
}
