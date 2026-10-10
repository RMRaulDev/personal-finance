import { StyleSheet } from 'react-native';

import { space } from '../../../design-system';

/** Styles of `DetailRow`: the label and value row, and the value that wraps on the right. */
export function createDetailRowStyles() {
  return StyleSheet.create({
    row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: space[3] },
    value: { flexShrink: 1, textAlign: 'right' },
  });
}
