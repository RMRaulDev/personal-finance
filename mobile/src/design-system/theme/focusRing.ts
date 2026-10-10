import type { ViewStyle } from 'react-native';

import { borderWidth } from '../tokens';

import type { Theme } from './ThemeProvider';

/** Design system focus ring: 2px in `border-focus` with a 2px offset. Never remove it. */
export function focusRing(theme: Theme): ViewStyle {
  return {
    outlineStyle: 'solid',
    outlineWidth: borderWidth.focus,
    outlineOffset: borderWidth.focus,
    outlineColor: theme.colors.border.focus,
  };
}
