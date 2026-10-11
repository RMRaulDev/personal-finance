import type { ReactNode } from 'react';
import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { space, useThemedStyles } from '../../../design-system';

import { createBottomBarStyles } from './BottomBar.styles';

/** Props of `BottomBar`. */
export interface BottomBarProps {
  /** The bar's buttons, in a row. A single primary action takes the full width with `fullWidth`. */
  children: ReactNode;
}

/**
 * Fixed bar at the bottom of a screen, from the screen designs: a raised surface with a subtle top
 * border, holding the screen's main actions (e.g. "Ingreso" and "+ Gasto", or "Agregar compromiso").
 * Meant for `Screen`'s `footer` slot; it pads the bottom safe area itself.
 */
export function BottomBar({ children }: BottomBarProps) {
  const styles = useThemedStyles(createBottomBarStyles);
  const insets = useSafeAreaInsets();

  return <View style={[styles.bar, { paddingBottom: space[8] + insets.bottom }]}>{children}</View>;
}
