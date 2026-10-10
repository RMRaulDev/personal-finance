// Sizes from the "Dashboard Finanzas" design (Claude Design) that are not part of the design
// system's type or size scale and that several screens use. Values only one feature's screens use
// live in that feature's designSpec.ts. Everything else comes from the design system tokens.

/** Section titles ("Próximos compromisos", "Reciente"): 16px, line height 1.4, weight 600. */
export const sectionTitle = { fontSize: 16, lineHeight: 1.4 } as const;

/** Info icon of a notice: 20px, 1.5px stroke, nudged 2px down to align with the text. */
export const infoIcon = { size: 20, strokeWidth: 1.5, offsetTop: 2 } as const;

/**
 * Operation avatar: a 36px circle with an 18px, weight 500 glyph. The glyph's line height equals
 * its size so it centers the same way on Android and iOS.
 */
export const operationAvatar = { size: 36, glyphSize: 18, glyphLineHeight: 18 } as const;

/** How long a toast stays on screen, in milliseconds. */
export const toastDurationMs = 4000;

/**
 * Longer toast duration while a screen reader is on, so its user has time to reach "Deshacer".
 * Not part of the design, which only covers sighted use.
 */
export const toastDurationScreenReaderMs = 10000;
