import { useState, type ReactNode } from 'react';
import { Pressable, View } from 'react-native';

import { useThemedStyles } from '../../theme';

import { Chevron } from './Chevron';
import { createPressableRowStyles } from './PressableRow.styles';

/** `PressableRow` props. */
export interface PressableRowProps {
  /** The row's texts, in a column that takes the free width. */
  children: ReactNode;
  /** Content before the chevron, such as an amount. */
  trailing?: ReactNode;
  /**
   * Summary of the whole row for screen readers, because the row is a single button (e.g. "Gimnasio,
   * $550, Mensual · día 5, 2 vencidas, desde el 5 sep").
   */
  accessibilityLabel: string;
  /** Opens the detail. */
  onPress: () => void;
}

/**
 * A list row that opens a detail: the whole row is one button and the chevron says it leads to
 * another screen. Rows hold no buttons of their own. Meant for a list group, which draws the
 * dividers. Proposal ("Fila clicable") from the "Compromisos v1" screen design, pending adoption in
 * the Lucas DS source project; it settles the pending clickable card only for lists.
 */
export function PressableRow({ children, trailing, accessibilityLabel, onPress }: PressableRowProps) {
  const styles = useThemedStyles(createPressableRowStyles);
  const [focused, setFocused] = useState(false);

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel}
      onPress={onPress}
      onFocus={() => setFocused(true)}
      onBlur={() => setFocused(false)}
      style={({ pressed }) => [styles.row, pressed && styles.pressed, focused && styles.focused]}
    >
      <View style={styles.content}>{children}</View>
      {trailing}
      <Chevron />
    </Pressable>
  );
}
