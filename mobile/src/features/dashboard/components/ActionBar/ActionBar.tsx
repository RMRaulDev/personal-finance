import { View } from 'react-native';

import { Button, useThemedStyles } from '../../../../design-system';
import { BottomBar } from '../../../../shared';

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

  return (
    <BottomBar>
      <Button variant="ghost" accessibilityLabel="Registrar ingreso" onPress={onIncome}>
        Ingreso
      </Button>
      {/* The primary action takes the rest of the row. */}
      <View style={styles.primarySlot}>
        <Button variant="primary" size="lg" fullWidth accessibilityLabel="Registrar gasto" onPress={onExpense}>
          + Gasto
        </Button>
      </View>
    </BottomBar>
  );
}
