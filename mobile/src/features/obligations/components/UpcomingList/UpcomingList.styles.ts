import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/** Styles of `UpcomingList`: the section with its title above the group. */
export function createUpcomingListStyles() {
  return StyleSheet.create({
    section: { gap: space[3] },
  });
}
