import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/** Styles of `OverdueNotice`: the two stacked buttons, a step further from the text, as in the design. */
export function createOverdueNoticeStyles() {
  return StyleSheet.create({
    actions: { gap: space[2], marginTop: space[1] },
  });
}
