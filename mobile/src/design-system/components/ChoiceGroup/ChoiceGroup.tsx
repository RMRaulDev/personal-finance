import { View } from 'react-native';

import { useThemedStyles } from '../../theme';
import { Text } from '../Text';

import { Chip } from './Chip';
import { createChoiceGroupStyles } from './ChoiceGroup.styles';

/** One option of a `ChoiceGroup`. */
export interface ChoiceOption<T extends string> {
  value: T;
  /** Short visible text, 1–3 words. */
  label: string;
}

/** `ChoiceGroup` props: the label, the options, and the selected value. */
export interface ChoiceGroupProps<T extends string> {
  /** Visible label; also the radio group's accessible name. */
  label: string;
  /**
   * Hides the label while keeping it as the radio group's accessible name, for chips that sit
   * under a control that already has a visible label (e.g. the quick dates of `DateField`).
   * Defaults to false.
   */
  labelHidden?: boolean;
  /** Up to 12 options, in the order they are shown. */
  options: ChoiceOption<T>[];
  /** Selected value, or null when nothing is selected. */
  value: T | null;
  /** Receives the chosen value, or null when `allowDeselect` lets the user clear the selection. */
  onChange: (value: T | null) => void;
  /** Tapping the selected chip clears the selection (for optional choices). Defaults to false. */
  allowDeselect?: boolean;
  /** What is missing and what to do, in one sentence. Shown below the chips. */
  error?: string;
}

/**
 * Single choice among a few short options, as wrapping chips (a radio group). Proposal from the
 * "Onboarding v1" screen design, pending adoption in the Lucas DS source project.
 */
export function ChoiceGroup<T extends string>({
  label,
  labelHidden = false,
  options,
  value,
  onChange,
  allowDeselect = false,
  error,
}: ChoiceGroupProps<T>) {
  const styles = useThemedStyles(createChoiceGroupStyles);

  return (
    <View style={styles.group}>
      {!labelHidden && (
        <Text variant="body-sm" weight={500}>
          {label}
        </Text>
      )}
      <View accessibilityRole="radiogroup" accessibilityLabel={label} style={styles.chips}>
        {options.map((option) => {
          const selected = option.value === value;
          return (
            <Chip
              key={option.value}
              label={option.label}
              selected={selected}
              onPress={() => {
                if (!selected) {
                  onChange(option.value);
                } else if (allowDeselect) {
                  onChange(null);
                }
              }}
            />
          );
        })}
      </View>
      {error !== undefined && (
        <Text variant="body-sm" color="danger">
          {error}
        </Text>
      )}
    </View>
  );
}
