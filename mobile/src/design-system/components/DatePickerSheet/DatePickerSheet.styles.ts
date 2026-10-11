import { StyleSheet } from 'react-native';

import type { Theme } from '../../theme';
import { radius, space } from '../../tokens';

/** Grab handle at the top of the sheet, from the screen designs: 36×4, in `border-default`. */
const SHEET_HANDLE = { width: 36, height: 4 } as const;

/**
 * Styles of `DatePickerSheet`: the backdrop, the bottom sheet and its handle, the month navigation,
 * the weekday header, the grid rows and their blank slots, and the actions. The bottom padding includes the safe area, so
 * the component adds it.
 */
export function createDatePickerSheetStyles(theme: Theme) {
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
      gap: space[4],
      paddingTop: space[3],
      paddingHorizontal: space[5],
      backgroundColor: colors.surface.overlay,
      borderTopLeftRadius: radius.xl,
      borderTopRightRadius: radius.xl,
      // A floating element, one of the few allowed a shadow.
      boxShadow: theme.shadows.lg,
    },
    handle: {
      alignSelf: 'center',
      width: SHEET_HANDLE.width,
      height: SHEET_HANDLE.height,
      borderRadius: radius.full,
      backgroundColor: colors.border.default,
    },
    navigation: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
    grid: { gap: space[1] },
    week: { flexDirection: 'row' },
    weekdayHeader: { flex: 1, height: space[6], alignItems: 'center', justifyContent: 'center' },
    // A day of the neighbouring months: an empty slot that keeps the columns aligned.
    blank: { flex: 1 },
    actions: { flexDirection: 'row', gap: space[2] },
    primarySlot: { flex: 1 },
  });
}
