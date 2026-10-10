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
