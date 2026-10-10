import { StyleSheet } from 'react-native';

import { space } from '../../design-system';

/** Styles of `DashboardScreen`: the padding of the fixed date header (`Screen` does not pad it). */
export function createDashboardScreenStyles() {
  return StyleSheet.create({
    header: { paddingTop: space[2], paddingRight: space[4], paddingLeft: space[5] },
  });
}
