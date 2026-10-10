import { StyleSheet } from 'react-native';

import { space } from '../../../design-system';

/** Styles of `CenteredState`: the full screen with its content centered. */
export function createCenteredStateStyles() {
  return StyleSheet.create({
    screen: { alignItems: 'center', justifyContent: 'center', padding: space[6] },
  });
}
