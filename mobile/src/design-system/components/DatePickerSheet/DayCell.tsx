import { useState } from 'react';
import { Pressable, View } from 'react-native';

import { useThemedStyles } from '../../theme';
import { Text } from '../Text';

import { createDayCellStyles } from './DayCell.styles';

/** Props of `DayCell`. */
interface DayCellProps {
  /** Day of the month shown in the cell. */
  label: string;
  /** Weekday, date, and state for screen readers ("Jueves 15 oct, hoy, elegida"). */
  accessibilityLabel: string;
  today: boolean;
  selected: boolean;
  /** Outside the allowed range: drawn disabled and not pickable, but still announced. */
  disabled: boolean;
  onPress: () => void;
}

/** One day of the month grid. */
export function DayCell({ label, accessibilityLabel, today, selected, disabled, onPress }: DayCellProps) {
  const styles = useThemedStyles(createDayCellStyles);
  const [focused, setFocused] = useState(false);

  return (
    <View style={styles.slot}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={accessibilityLabel}
        accessibilityState={{ selected, disabled }}
        aria-disabled={disabled}
        onPress={() => {
          if (!disabled) {
            onPress();
          }
        }}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        style={({ pressed }) => [
          styles.cell,
          today && styles.today,
          pressed && !disabled && styles.pressed,
          selected && styles.selected,
          focused && styles.focused,
        ]}
      >
        <Text
          weight={today || selected ? 600 : 400}
          style={selected ? styles.selectedLabel : disabled ? styles.disabledLabel : styles.label}
        >
          {label}
        </Text>
      </Pressable>
    </View>
  );
}
