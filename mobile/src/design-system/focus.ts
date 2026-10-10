import type { ViewStyle } from 'react-native';

import type { Theme } from './theme';
import { borderWidth } from './tokens';

/** Anillo de foco del sistema: 2px en `border-focus` con 2px de separación. Nunca se quita. */
export function focusRing(theme: Theme): ViewStyle {
  return {
    outlineStyle: 'solid',
    outlineWidth: borderWidth.focus,
    outlineOffset: borderWidth.focus,
    outlineColor: theme.colors.border.focus,
  };
}
