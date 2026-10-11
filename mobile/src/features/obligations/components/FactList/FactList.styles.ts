import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/** Styles of `FactList`: the padding of each label and value row inside the list group. */
export function createFactListStyles() {
  return StyleSheet.create({
    row: { paddingVertical: space[3], paddingHorizontal: space[4] },
  });
}
