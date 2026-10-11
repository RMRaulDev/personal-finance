import { StyleSheet } from 'react-native';

import { radius, space, type Theme } from '../../../design-system';

// Sizes from the "Dashboard Finanzas" design (Claude Design) outside the design system's size
// scale. Everything else comes from the design system tokens.

/** Grab handle at the top of a sheet: 36×4, in `border-default`. */
const SHEET_HANDLE = { width: 36, height: 4 } as const;

/** A sheet never grows past 94% of the screen height; its content scrolls. */
const SHEET_MAX_HEIGHT = '94%';

/**
 * Styles of `BottomSheet`: the backdrop, the sheet and its handle, and the scrolling content. The
 * content's bottom padding depends on the keyboard and the safe area, so the component adds it.
 */
export function createBottomSheetStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    container: { flex: 1, justifyContent: 'flex-end' },
    backdrop: {
      position: 'absolute',
      top: 0,
      right: 0,
      bottom: 0,
      left: 0,
      // The scrim token darkens the screen in both themes.
      backgroundColor: colors.scrim,
    },
    sheet: {
      maxHeight: SHEET_MAX_HEIGHT,
      backgroundColor: colors.surface.overlay,
      borderTopLeftRadius: radius.xl,
      borderTopRightRadius: radius.xl,
      // A floating element, one of the few allowed a shadow.
      boxShadow: theme.shadows.lg,
    },
    content: {
      gap: space[5],
      paddingTop: space[3],
      paddingHorizontal: space[5],
    },
    handle: {
      alignSelf: 'center',
      width: SHEET_HANDLE.width,
      height: SHEET_HANDLE.height,
      borderRadius: radius.full,
      backgroundColor: colors.border.default,
    },
  });
}
