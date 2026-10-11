import { StyleSheet } from 'react-native';

import { space } from '../../../design-system';

/** Styles of `ObligationDetailScreen`: the title block, the name and badge row, and the sections. */
export function createObligationDetailScreenStyles() {
  return StyleSheet.create({
    titleBlock: { gap: space[2] },
    nameRow: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: space[2] },
    section: { gap: space[3] },
    manage: { gap: space[2] },
  });
}
