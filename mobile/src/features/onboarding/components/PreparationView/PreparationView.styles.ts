import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/**
 * Bottom padding of the preparation screen, from the "Onboarding v1" design (Claude Design),
 * outside the design system's spacing scale: it lifts the centered content above the middle.
 */
const PREPARATION_BOTTOM_PADDING = 120;

/** Styles of `PreparationView`: the centered column and its centered texts. */
export function createPreparationViewStyles() {
  return StyleSheet.create({
    view: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      gap: space[5],
      paddingHorizontal: space[8],
      paddingBottom: PREPARATION_BOTTOM_PADDING,
    },
    texts: { gap: space[2] },
    centered: { textAlign: 'center' },
  });
}
