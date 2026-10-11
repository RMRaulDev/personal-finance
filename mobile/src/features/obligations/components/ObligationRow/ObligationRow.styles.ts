import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/** Styles of `ObligationRow`: the text column, the badge and status line, and the top-aligned amount. */
export function createObligationRowStyles() {
  return StyleSheet.create({
    texts: { gap: space[0.5] },
    statusLine: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      alignItems: 'center',
      gap: space[2],
      marginTop: space[0.5],
    },
    amount: { alignSelf: 'flex-start' },
  });
}
