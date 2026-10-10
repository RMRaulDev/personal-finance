import { StyleSheet } from 'react-native';

/**
 * Section title size from the "Dashboard Finanzas" design (Claude Design), outside the design
 * system's type scale: 16px with a 1.4 line height (weight 600, set by the component).
 */
const SECTION_TITLE = { fontSize: 16, lineHeight: 1.4 } as const;

/** Styles of `SectionTitle`: the design's size, line height, and no tracking. */
export function createSectionTitleStyles() {
  return StyleSheet.create({
    title: {
      fontSize: SECTION_TITLE.fontSize,
      lineHeight: SECTION_TITLE.fontSize * SECTION_TITLE.lineHeight,
      letterSpacing: 0,
    },
  });
}
