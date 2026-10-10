import { StyleSheet } from 'react-native';

/**
 * Info icon from the "Dashboard Finanzas" design (Claude Design), outside the design system's
 * size scale: 20px with a 1.5px stroke, nudged 2px down to align with the text.
 */
export const INFO_ICON = { size: 20, strokeWidth: 1.5, offsetTop: 2 } as const;

/** Styles of `InfoIcon`: the nudge that aligns it with the first line of text. */
export function createInfoIconStyles() {
  return StyleSheet.create({
    icon: { marginTop: INFO_ICON.offsetTop },
  });
}
