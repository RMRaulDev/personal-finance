import { StyleSheet } from 'react-native';

import { control, space } from '../../../design-system';

/** Styles of `BackHeader`: the fixed 48px row that holds the back button (`Screen` does not pad it). */
export function createBackHeaderStyles() {
  return StyleSheet.create({
    header: { height: control.lg, flexDirection: 'row', alignItems: 'center', paddingHorizontal: space[3] },
  });
}
