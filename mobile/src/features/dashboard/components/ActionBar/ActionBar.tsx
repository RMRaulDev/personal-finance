import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button, space, useThemedStyles } from '../../../../design-system';

import { createActionBarStyles } from './ActionBar.styles';

/** Props of `ActionBar`. */
export interface ActionBarProps {
  /** Opens the registration sheet for an income. */
  onIncome: () => void;
  /** Opens the registration sheet for an expense: the screen's primary action. */
  onExpense: () => void;
}

/**
 * Bottom action bar of the dashboard: "Ingreso" and "+ Gasto". "Transferir" stays hidden until
 * the transfer screen has a design (the design itself hides it with a single account).
 */
export function ActionBar({ onIncome, onExpense }: ActionBarProps) {
  const styles = useThemedStyles(createActionBarStyles);
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.bar, { paddingBottom: space[8] + insets.bottom }]}>
      <Button variant="ghost" accessibilityLabel="Registrar ingreso" onPress={onIncome}>
        Ingreso
      </Button>
      {/* The primary action takes the rest of the row. */}
      <View style={styles.primarySlot}>
        <Button variant="primary" size="lg" fullWidth accessibilityLabel="Registrar gasto" onPress={onExpense}>
          + Gasto
        </Button>
      </View>
    </View>
  );
}
