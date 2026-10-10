import { StyleSheet } from 'react-native';

import { borderWidth, radius, space, type Theme } from '../../../design-system';

/**
 * Operation avatar from the "Dashboard Finanzas" design (Claude Design), outside the design
 * system's size scale: a 36px circle with an 18px, weight 500 glyph. The glyph's line height
 * equals its size so it centers the same way on Android and iOS.
 */
const OPERATION_AVATAR = { size: 36, glyphSize: 18, glyphLineHeight: 18 } as const;

/** Styles of `OperationRow`: the row, the avatar and its glyph, and the label column. */
export function createOperationRowStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    row: { flexDirection: 'row', alignItems: 'center', gap: space[3], paddingVertical: space[2] },
    avatar: {
      width: OPERATION_AVATAR.size,
      height: OPERATION_AVATAR.size,
      borderRadius: radius.full,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.subtle,
      backgroundColor: colors.surface.raised,
      alignItems: 'center',
      justifyContent: 'center',
    },
    glyph: {
      fontSize: OPERATION_AVATAR.glyphSize,
      lineHeight: OPERATION_AVATAR.glyphLineHeight,
      // Android adds font padding above and below the text, which pushes the glyph off center.
      includeFontPadding: false,
      textAlignVertical: 'center',
    },
    texts: { flex: 1, minWidth: 0 },
  });
}
