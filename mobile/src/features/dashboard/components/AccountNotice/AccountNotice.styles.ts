import { StyleSheet } from 'react-native';

import { control, focusRing, radius, space, type Theme } from '../../../../design-system';

/** Styles of `AccountNotice`: its pull toward the amount, and the "y N avisos más" link and its focus. */
export function createAccountNoticeStyles(theme: Theme) {
  return StyleSheet.create({
    // Pulled up toward the available amount, as in the design, because it qualifies it.
    notice: { marginTop: -space[4] },
    link: {
      alignSelf: 'center',
      justifyContent: 'center',
      // The text is small, but the touch target keeps the 48px mobile control height.
      minHeight: control.lg,
      paddingHorizontal: space[3],
      borderRadius: radius.md,
    },
    linkFocused: focusRing(theme),
    linkText: { textDecorationLine: 'underline' },
  });
}
