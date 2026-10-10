// Tokens del sistema de diseño de Raúl ("Lucas DS" en claude.ai/design), traducidos de tokens.css.
// 1rem = 16px. Consume solo los semánticos (useTheme().colors); los primitivos son cableado.

const neutral = {
  50: '#f1f9f7',
  100: '#e4ebea',
  200: '#cfd6d4',
  300: '#b5bcbb',
  400: '#9ba2a0',
  500: '#818886',
  600: '#6a706f',
  700: '#535a58',
  800: '#3f4544',
  850: '#3b4140',
  875: '#353b39',
  900: '#2c3231',
  925: '#212725',
  950: '#1a1f1e',
} as const;

const teal = {
  50: '#eafbf8',
  100: '#d0f2eb',
  200: '#a4e3d7',
  300: '#6dcfbf',
  400: '#32b7a5',
  500: '#079b8b',
  600: '#048073',
  700: '#01675b',
  800: '#014f46',
  900: '#033932',
  950: '#002420',
} as const;

const success = {
  50: '#eefbee',
  100: '#d8f2d8',
  200: '#b3e3b3',
  300: '#88cf8a',
  400: '#61b766',
  500: '#409d48',
  600: '#2b8334',
  700: '#1b6a25',
  800: '#11521a',
  900: '#073b0f',
  950: '#012605',
} as const;

const warning = {
  50: '#fef5e9',
  100: '#fce6c9',
  200: '#f5cd97',
  300: '#e7af5c',
  400: '#d3911a',
  500: '#b37903',
  600: '#946305',
  700: '#774f04',
  800: '#5c3c02',
  900: '#432b01',
  950: '#2b1a01',
} as const;

const danger = {
  50: '#fff4f3',
  100: '#fee2df',
  200: '#fec4be',
  300: '#fe9b93',
  400: '#f4726b',
  500: '#da534f',
  600: '#ba3d3b',
  700: '#982c2b',
  800: '#781f1f',
  900: '#591313',
  950: '#3c0708',
} as const;

const info = {
  50: '#f1f8fe',
  100: '#d9ecfe',
  200: '#b0dafe',
  300: '#7ac2fe',
  400: '#4ea7ed',
  500: '#288dd4',
  600: '#0974b6',
  700: '#025c93',
  800: '#004773',
  900: '#003355',
  950: '#012037',
} as const;

export const primitives = { neutral, teal, success, warning, danger, info } as const;

export type ColorScheme = 'light' | 'dark';
export type Role = 'accent' | 'success' | 'warning' | 'danger' | 'info';
export const roles: readonly Role[] = ['accent', 'success', 'warning', 'danger', 'info'];

type Ramp = Record<keyof typeof teal, string>;
const roleRamps: Record<Role, Ramp> = { accent: teal, success, warning, danger, info };

export interface RoleColors {
  fill: string;
  fillHover: string;
  /** Texto sobre `fill`. En oscuro es texto oscuro a propósito. */
  on: string;
  bg: string;
  border: string;
  text: string;
}

export interface SemanticColors {
  surface: { page: string; raised: string; overlay: string; hover: string; selected: string };
  text: { primary: string; secondary: string; disabled: string };
  border: { subtle: string; default: string; control: string; focus: string };
  role: Record<Role, RoleColors>;
}

// Regla de pasos de los roles: fill 600/400, hover 700/300, bg 50/950, border 200/800, text 700/300.
function roleColors(scheme: ColorScheme): Record<Role, RoleColors> {
  const light = scheme === 'light';
  const entries = roles.map((role) => {
    const ramp = roleRamps[role];
    const colors: RoleColors = {
      fill: light ? ramp[600] : ramp[400],
      fillHover: light ? ramp[700] : ramp[300],
      on: light ? '#ffffff' : neutral[950],
      bg: light ? ramp[50] : ramp[950],
      border: light ? ramp[200] : ramp[800],
      text: light ? ramp[700] : ramp[300],
    };
    return [role, colors] as const;
  });
  return Object.fromEntries(entries) as Record<Role, RoleColors>;
}

export const colors: Record<ColorScheme, SemanticColors> = {
  light: {
    surface: {
      page: neutral[50],
      raised: '#ffffff',
      overlay: '#ffffff',
      hover: neutral[100],
      selected: neutral[200],
    },
    text: { primary: neutral[900], secondary: neutral[600], disabled: neutral[300] },
    border: { subtle: neutral[200], default: neutral[300], control: neutral[500], focus: teal[600] },
    role: roleColors('light'),
  },
  dark: {
    surface: {
      page: neutral[950],
      raised: neutral[925],
      overlay: neutral[900],
      hover: neutral[875],
      selected: neutral[850],
    },
    text: { primary: neutral[50], secondary: neutral[400], disabled: neutral[700] },
    border: { subtle: neutral[800], default: neutral[700], control: neutral[500], focus: teal[400] },
    role: roleColors('dark'),
  },
};

/** Sombras solo para lo que flota (popovers, menús, diálogos). Las cards nunca llevan sombra. */
export const shadows: Record<ColorScheme, { md: string; lg: string }> = {
  light: {
    md: '0 2px 4px #1A1F1E0F, 0 8px 16px #1A1F1E14',
    lg: '0 4px 8px #1A1F1E14, 0 16px 32px #1A1F1E1F',
  },
  dark: {
    md: '0 2px 4px #0000004D, 0 8px 16px #00000066',
    lg: '0 4px 8px #00000066, 0 16px 32px #00000080',
  },
};

/** Base 4px; la clave es el multiplicador (`space[4]` = 16). `--space-0-5` es `space[0.5]`. */
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

export const radius = { none: 0, sm: 4, md: 6, lg: 8, xl: 12, full: 9999 } as const;

export const control = { sm: 32, md: 40, lg: 48 } as const;

/** Grosores permitidos como literales: bordes de 1px y anillo de foco de 2px. */
export const borderWidth = { hairline: 1, focus: 2 } as const;

// Inter (interfaz) y JetBrains Mono (código y datos). En React Native cada peso es una familia distinta.
export const fontFamily = {
  sans: { 400: 'Inter_400Regular', 500: 'Inter_500Medium', 600: 'Inter_600SemiBold' },
  mono: 'JetBrainsMono_400Regular',
} as const;

export type FontWeight = keyof typeof fontFamily.sans;

interface FixedTextStyle {
  size: number;
  leading: number;
  weight: FontWeight;
  /** En em, como en tokens.css. */
  tracking?: number;
}

/** `clamp(min, base + vw·ancho/100, max)`, el mismo cálculo que el CSS fluido. */
export interface Fluid {
  min: number;
  base: number;
  vw: number;
  max: number;
}

interface FluidTextStyle extends Omit<FixedTextStyle, 'size'> {
  size: Fluid;
}

export const textStyles = {
  // Fluidos: solo de página.
  display: { size: { min: 32, base: 25.744, vw: 1.739, max: 48 }, leading: 1.15, weight: 600, tracking: -0.02 },
  'title-1': { size: { min: 28, base: 24.864, vw: 0.87, max: 36 }, leading: 1.2, weight: 600, tracking: -0.02 },
  'title-2': { size: { min: 24, base: 21.648, vw: 0.652, max: 30 }, leading: 1.25, weight: 600, tracking: -0.02 },
  // Fijos: de componente.
  heading: { size: 20, leading: 1.3, weight: 600, tracking: -0.02 },
  subheading: { size: 18, leading: 1.4, weight: 500 },
  body: { size: 16, leading: 1.5, weight: 400 },
  'body-sm': { size: 14, leading: 1.43, weight: 400 },
  caption: { size: 12, leading: 1.33, weight: 500 },
} as const satisfies Record<string, FixedTextStyle | FluidTextStyle>;

export type TextVariant = keyof typeof textStyles;

/** `--text-code-size: 0.875em`: relativo al texto que lo rodea. */
export const codeScale = 0.875;

/** Solo separa secciones de página. */
export const spaceSection: Fluid = { min: 48, base: 29.216, vw: 5.217, max: 96 };

export function resolveFluid(value: Fluid, viewportWidth: number): number {
  const fluid = value.base + (value.vw * viewportWidth) / 100;
  return Math.min(value.max, Math.max(value.min, fluid));
}
