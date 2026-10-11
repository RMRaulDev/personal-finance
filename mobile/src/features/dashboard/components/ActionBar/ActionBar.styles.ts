import { StyleSheet } from 'react-native';

/**
 * Styles of `ActionBar`: the slot that gives the primary action the rest of the row. The bar itself
 * is the shared `BottomBar`.
 */
export function createActionBarStyles() {
  return StyleSheet.create({
    primarySlot: { flex: 1 },
  });
}
