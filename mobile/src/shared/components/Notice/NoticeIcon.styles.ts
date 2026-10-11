import { StyleSheet } from 'react-native';

/**
 * Notice icons from the "Dashboard Finanzas" and "Compromisos v1" designs (Claude Design), outside
 * the design system's size scale: 20px with a 1.5px stroke, nudged 2px down to align with the text.
 */
export const NOTICE_ICON = { size: 20, strokeWidth: 1.5, offsetTop: 2 } as const;

/** Styles of `NoticeIcon`: the nudge that aligns it with the first line of text. */
export function createNoticeIconStyles() {
  return StyleSheet.create({
    icon: { marginTop: NOTICE_ICON.offsetTop },
  });
}
