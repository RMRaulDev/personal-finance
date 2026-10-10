// Spacing, radii, control heights, and border widths of Lucas DS, translated from tokens.css.
// 1rem = 16px.

import type { Fluid } from './typography';

/** 4px base; the key is the multiplier (`space[4]` = 16). `--space-0-5` is `space[0.5]`. */
export const space = {
  0: 0,
  0.5: 2,
  1: 4,
  2: 8,
  3: 12,
  4: 16,
  5: 20,
  6: 24,
  8: 32,
  10: 40,
  12: 48,
  16: 64,
  20: 80,
  24: 96,
} as const;

/** Radii: `sm` badges, `md` controls, `lg` cards, `xl` overlays, `full` pills. They do not scale with size. */
export const radius = { none: 0, sm: 4, md: 6, lg: 8, xl: 12, full: 9999 } as const;

/**
 * Control heights: `sm` tables and toolbars, `md` default, `lg` mobile forms and CTAs, `xl` the
 * amount field, and `touch` the minimum touch target of compact controls such as choice chips.
 * `xl` and `touch` are proposals pending adoption in the Lucas DS source project.
 */
export const control = { sm: 32, md: 40, lg: 48, xl: 64, touch: 44 } as const;

/** The only widths allowed as literals: 1px borders and the 2px focus ring. */
export const borderWidth = { hairline: 1, focus: 2 } as const;

/** Only separates page sections. */
export const spaceSection: Fluid = { min: 48, base: 29.216, vw: 5.217, max: 96 };
