import { StyleSheet } from 'react-native';

import { borderWidth, control, radius, space, type Theme } from '../../../../design-system';

// Sizes from the "Dashboard Finanzas" design (Claude Design) outside the design system's type or
// size scale. Everything else comes from the design system tokens.

/** Hero amount "Disponible para gastar": 52px, line height 1.05, tracking −0.03em, weight 600. */
const HERO_AMOUNT = { fontSize: 52, lineHeight: 1.05, tracking: -0.03 } as const;

/** Max width of the "faltan $X para cubrir tus compromisos" note next to the hero amount. */
const SHORTFALL_NOTE_MAX_WIDTH = 170;

/** Styles of `AvailableSummary`: the block, the hero amount row, the shortfall note, and the horizon chip. */
export function createAvailableSummaryStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    summary: { gap: space[2] },
    amountRow: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'baseline', gap: space[3] },
    heroAmount: {
      fontSize: HERO_AMOUNT.fontSize,
      lineHeight: HERO_AMOUNT.fontSize * HERO_AMOUNT.lineHeight,
      letterSpacing: HERO_AMOUNT.fontSize * HERO_AMOUNT.tracking,
    },
    shortfallNote: { maxWidth: SHORTFALL_NOTE_MAX_WIDTH },
    horizonChip: {
      marginTop: space[2],
      alignSelf: 'flex-start',
      justifyContent: 'center',
      height: control.sm,
      paddingHorizontal: space[3],
      borderRadius: radius.full,
      borderWidth: borderWidth.hairline,
      borderColor: colors.border.default,
      backgroundColor: colors.surface.raised,
    },
  });
}
