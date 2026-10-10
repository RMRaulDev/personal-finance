import { StyleSheet } from 'react-native';

import { space } from '../../tokens';

/** Styles of `ChoiceGroup`: the column with its label and error, and the wrapping row of chips. */
export function createChoiceGroupStyles() {
  return StyleSheet.create({
    group: { flexDirection: 'column', gap: space[2] },
    chips: { flexDirection: 'row', flexWrap: 'wrap', gap: space[2] },
  });
}
