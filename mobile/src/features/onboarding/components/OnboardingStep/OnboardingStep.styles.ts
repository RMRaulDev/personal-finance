import { StyleSheet } from 'react-native';

import { control, space } from '../../../../design-system';

/**
 * Styles of `OnboardingStep`: the header, the scrolling body, and the footer with one entry per
 * footer padding (`footerRelaxed`, `footerKeyboard`). The footer's bottom padding includes the
 * safe area, so the component adds it.
 */
export function createOnboardingStepStyles() {
  return StyleSheet.create({
    header: {
      height: control.lg,
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: space[3],
    },
    backSlot: { flexDirection: 'row' },
    stepLabel: { paddingHorizontal: space[2] },
    body: { flex: 1 },
    bodyContent: { paddingVertical: space[4], paddingHorizontal: space[5], gap: space[8] },
    intro: { gap: space[3] },
    footer: { paddingHorizontal: space[5] },
    footerRelaxed: { paddingTop: space[4] },
    footerKeyboard: { paddingTop: space[3] },
  });
}
