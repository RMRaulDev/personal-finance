import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/** Styles of `SummaryLine`: the row padding inside the summary group. */
export function createSummaryLineStyles() {
  return StyleSheet.create({
    line: { paddingVertical: space[3], paddingHorizontal: space[4] },
  });
}
