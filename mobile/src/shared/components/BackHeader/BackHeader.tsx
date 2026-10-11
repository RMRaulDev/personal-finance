import { View } from 'react-native';

import { Button, useThemedStyles } from '../../../design-system';

import { createBackHeaderStyles } from './BackHeader.styles';

/** Props of `BackHeader`. */
export interface BackHeaderProps {
  /** Where it goes, as the design writes it: "‹ Inicio", "‹ Compromisos", or "Cancelar". */
  label: string;
  /** Goes back; Android's Back does the same. */
  onPress: () => void;
}

/**
 * Fixed header of a screen reached from another one: a ghost text button that goes back. Pass it
 * as `Screen`'s `header`, which is not padded.
 */
export function BackHeader({ label, onPress }: BackHeaderProps) {
  const styles = useThemedStyles(createBackHeaderStyles);

  return (
    <View style={styles.header}>
      <Button variant="ghost" onPress={onPress}>
        {label}
      </Button>
    </View>
  );
}
