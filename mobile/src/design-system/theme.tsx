import { createContext, useContext, useMemo, type ReactNode } from 'react';
import { useColorScheme } from 'react-native';

import { colors, shadows, type ColorScheme, type SemanticColors } from './tokens';

export interface Theme {
  scheme: ColorScheme;
  colors: SemanticColors;
  shadows: { md: string; lg: string };
}

const ThemeContext = createContext<Theme | null>(null);

interface ThemeProviderProps {
  /** Fuerza un tema (equivale a `data-theme`). Sin valor sigue el tema del sistema. */
  scheme?: ColorScheme;
  children: ReactNode;
}

export function ThemeProvider({ scheme, children }: ThemeProviderProps) {
  const systemScheme = useColorScheme();
  const resolved: ColorScheme = scheme ?? (systemScheme === 'dark' ? 'dark' : 'light');
  const theme = useMemo<Theme>(
    () => ({ scheme: resolved, colors: colors[resolved], shadows: shadows[resolved] }),
    [resolved],
  );
  return <ThemeContext.Provider value={theme}>{children}</ThemeContext.Provider>;
}

export function useTheme(): Theme {
  const theme = useContext(ThemeContext);
  if (!theme) {
    throw new Error('useTheme must be used inside <DesignSystemProvider>');
  }
  return theme;
}
