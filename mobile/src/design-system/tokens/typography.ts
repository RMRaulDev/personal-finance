// Typography of Lucas DS, translated from tokens.css: font families, weights, the type scale,
// and the fluid calculation shared with `spaceSection`. 1rem = 16px.

/** Inter (interface) and JetBrains Mono (code and data). In React Native each weight is a separate family. */
export const fontFamily = {
  sans: { 400: 'Inter_400Regular', 500: 'Inter_500Medium', 600: 'Inter_600SemiBold' },
  mono: 'JetBrainsMono_400Regular',
} as const;

/** Allowed weights: 400 text, 500 UI and labels, 600 titles. Never 700. */
export type FontWeight = keyof typeof fontFamily.sans;

interface FixedTextStyle {
  size: number;
  leading: number;
  weight: FontWeight;
  /** In em, as in tokens.css. */
  tracking?: number;
}

/** `clamp(min, base + vw·width/100, max)`, the same calculation as the fluid CSS. */
export interface Fluid {
  min: number;
  base: number;
  vw: number;
  max: number;
}

interface FluidTextStyle extends Omit<FixedTextStyle, 'size'> {
  size: Fluid;
}

/** Type scale: size in px, relative line height, weight, and tracking in em. */
export const textStyles = {
  // Fluid: page-level only.
  display: { size: { min: 32, base: 25.744, vw: 1.739, max: 48 }, leading: 1.15, weight: 600, tracking: -0.02 },
  'title-1': { size: { min: 28, base: 24.864, vw: 0.87, max: 36 }, leading: 1.2, weight: 600, tracking: -0.02 },
  'title-2': { size: { min: 24, base: 21.648, vw: 0.652, max: 30 }, leading: 1.25, weight: 600, tracking: -0.02 },
  // Fixed: component-level.
  heading: { size: 20, leading: 1.3, weight: 600, tracking: -0.02 },
  subheading: { size: 18, leading: 1.4, weight: 500 },
  body: { size: 16, leading: 1.5, weight: 400 },
  'body-sm': { size: 14, leading: 1.43, weight: 400 },
  caption: { size: 12, leading: 1.33, weight: 500 },
  // Fixed, never fluid: the amount typed in `AmountField`. Proposal pending adoption in Lucas DS.
  amount: { size: 36, leading: 1.15, weight: 600, tracking: -0.02 },
} as const satisfies Record<string, FixedTextStyle | FluidTextStyle>;

/** Name of a type style (`.text-*` in the web version). */
export type TextVariant = keyof typeof textStyles;

/** `--text-code-size: 0.875em`: relative to the surrounding text. */
export const codeScale = 0.875;

/** Computes a fluid value for a screen width, clamped between its minimum and maximum. */
export function resolveFluid(value: Fluid, viewportWidth: number): number {
  const fluid = value.base + (value.vw * viewportWidth) / 100;
  return Math.min(value.max, Math.max(value.min, fluid));
}
