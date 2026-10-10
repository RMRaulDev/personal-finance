import { StyleSheet } from 'react-native';

/** Styles of `ScreenError`: the message and the retry button, centered. */
export function createScreenErrorStyles() {
  return StyleSheet.create({
    content: { alignItems: 'center' },
    message: { textAlign: 'center' },
  });
}
