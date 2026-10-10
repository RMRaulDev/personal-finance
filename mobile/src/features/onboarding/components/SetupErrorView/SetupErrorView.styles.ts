import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/**
 * Styles of `SetupErrorView`: the scrolling body, the message, and the footer with the two
 * buttons. The footer's bottom padding includes the safe area, so the component adds it.
 */
export function createSetupErrorViewStyles() {
  return StyleSheet.create({
    body: { flex: 1 },
    bodyContent: { paddingTop: space[16], paddingHorizontal: space[5], gap: space[6] },
    message: { gap: space[3] },
    footer: { paddingTop: space[4], paddingHorizontal: space[5], gap: space[2] },
  });
}
