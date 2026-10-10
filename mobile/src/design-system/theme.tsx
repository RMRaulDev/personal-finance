import { createContext, useContext, useMemo, type ReactNode } from 'react';
import { useColorScheme } from 'react-native';

import { colors, shadows, type ColorScheme, type SemanticColors } from './tokens';

/** Resolved theme: the active scheme with its semantic colors and shadows. */
export interface Theme {
  scheme: ColorScheme;
  colors: SemanticColors;
  shadows: { md: string; lg: string };
}

const ThemeContext = createContext<Theme | null>(null);

interface ThemeProviderProps {
  /** Forces a theme (equivalent to `data-theme`). Without a value it follows the system theme. */
  scheme?: ColorScheme;
  children: ReactNode;
}

/** Provides the theme to its children; follows the system theme unless `scheme` forces one. */
export function ThemeProvider({ scheme, children }: ThemeProviderProps) {
  const systemScheme = useColorScheme();
  const resolved: ColorScheme = scheme ?? (systemScheme === 'dark' ? 'dark' : 'light');
  const theme = useMemo<Theme>(
    () => ({ scheme: resolved, colors: colors[resolved], shadows: shadows[resolved] }),
    [resolved],
  );
  return <ThemeContext.Provider value={theme}>{children}</ThemeContext.Provider>;
}

/** Returns the active theme. Throws if used outside `DesignSystemProvider`. */
export function useTheme(): Theme {
  const theme = useContext(ThemeContext);
  if (!theme) {
    throw new Error('useTheme must be used inside <DesignSystemProvider>');
  }
  return theme;
}
