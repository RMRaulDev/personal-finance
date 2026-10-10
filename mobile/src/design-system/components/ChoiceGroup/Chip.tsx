import { useState } from 'react';
import { Pressable } from 'react-native';

import { useTheme, useThemedStyles } from '../../theme';
import { Text } from '../Text';

import { CheckIcon } from './CheckIcon';
import { createChipStyles } from './Chip.styles';

/** Props of `Chip`. */
interface ChipProps {
  label: string;
  selected: boolean;
  onPress: () => void;
}

/** One pill-shaped option: raised and outlined, or selected with a strong border and a check. */
export function Chip({ label, selected, onPress }: ChipProps) {
  const { colors } = useTheme();
  const styles = useThemedStyles(createChipStyles);
  const [focused, setFocused] = useState(false);

  return (
    <Pressable
      accessibilityRole="radio"
      accessibilityState={{ checked: selected }}
      accessibilityLabel={label}
      onPress={onPress}
      onFocus={() => setFocused(true)}
      onBlur={() => setFocused(false)}
      style={({ pressed }) => [
        styles.chip,
        pressed && styles.pressed,
        selected && styles.selected,
        focused && styles.focused,
      ]}
    >
      {selected && <CheckIcon color={colors.text.primary} />}
      {/* 14px / 500: the body-sm size with the UI weight; the text is always text-primary. */}
      <Text variant="body-sm" weight={500} numberOfLines={1}>
        {label}
      </Text>
    </Pressable>
  );
}
