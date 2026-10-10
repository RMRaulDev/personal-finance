import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { borderWidth, Button, space, useTheme } from '../../design-system';

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
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <View
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        gap: space[2],
        paddingTop: space[3],
        paddingHorizontal: space[4],
        paddingBottom: space[8] + insets.bottom,
        backgroundColor: colors.surface.raised,
        borderTopWidth: borderWidth.hairline,
        borderTopColor: colors.border.subtle,
      }}
    >
      <Button variant="ghost" accessibilityLabel="Registrar ingreso" onPress={onIncome}>
        Ingreso
      </Button>
      {/* The primary action takes the rest of the row. */}
      <View style={{ flex: 1 }}>
        <Button variant="primary" size="lg" fullWidth accessibilityLabel="Registrar gasto" onPress={onExpense}>
          + Gasto
        </Button>
      </View>
    </View>
  );
}
