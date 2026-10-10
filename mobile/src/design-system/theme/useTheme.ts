import { useContext } from 'react';

import { ThemeContext, type Theme } from './ThemeProvider';

/** Returns the active theme. Throws if used outside `DesignSystemProvider`. */
export function useTheme(): Theme {
  const theme = useContext(ThemeContext);
  if (!theme) {
    throw new Error('useTheme must be used inside <DesignSystemProvider>');
  }
  return theme;
}
