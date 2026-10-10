import { useMemo } from 'react';

import type { Theme } from './ThemeProvider';
import { useTheme } from './useTheme';

/**
 * Builds a component's styles from the active theme: pass the `createNameStyles` factory of its
 * `Name.styles.ts`. The styles are recomputed only when the theme changes (light or dark), so a
 * re-render reuses the same `StyleSheet`. The factory must be a module-level function, not one
 * created while rendering, or the memo would recompute on every render.
 */
export function useThemedStyles<T>(createStyles: (theme: Theme) => T): T {
  const theme = useTheme();
  return useMemo(() => createStyles(theme), [createStyles, theme]);
}
