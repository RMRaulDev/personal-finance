// Sizes from the "Dashboard Finanzas" design (Claude Design) that are not part of the design
// system's type or size scale and that only the dashboard uses. They live only here, so every
// deviation from the scale is visible in one place; values shared by several screens are in
// shared/designSpec.ts. Everything else comes from the design system tokens.

/** Hero amount "Disponible para gastar": 52px, line height 1.05, tracking −0.03em, weight 600. */
export const heroAmount = { fontSize: 52, lineHeight: 1.05, tracking: -0.03 } as const;

/** Max width of the "faltan $X para cubrir tus compromisos" note next to the hero amount. */
export const shortfallNoteMaxWidth = 170;

/** How many recent operations the design shows (the API returns up to 5). */
export const recentLimit = 3;

/** Grab handle at the top of the registration sheet: 36×4, in `border-default`. */
export const sheetHandle = { width: 36, height: 4 } as const;

/** The registration sheet never grows past 94% of the screen height; its content scrolls. */
export const sheetMaxHeight = '94%';
