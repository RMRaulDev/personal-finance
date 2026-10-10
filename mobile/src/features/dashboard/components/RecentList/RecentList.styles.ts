import { StyleSheet } from 'react-native';

import { space } from '../../../../design-system';

/** Styles of `RecentList`: the section and the extra gap under its title. */
export function createRecentListStyles() {
  return StyleSheet.create({
    list: { gap: space[1] },
    title: { marginBottom: space[1] },
  });
}
