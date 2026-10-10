import { useState } from 'react';
import { Pressable, View } from 'react-native';
import Svg, { Path } from 'react-native-svg';

import { focusRing } from './theme/focusRing';
import { Text } from './components/Text/Text';
import { useTheme } from './theme/ThemeProvider';
import { borderWidth, control, radius, space } from './tokens/colors';

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

/** Size of the check icon and its stroke, from the design's chip markup (16px, 1.75 stroke). */
const CHECK_ICON = { size: 16, strokeWidth: 1.75 } as const;

/** Props of `Chip`. */
interface ChipProps {
  label: string;
  selected: boolean;
  onPress: () => void;
}

/** Props of `CheckIcon`. */
interface CheckIconProps {
  color: string;
}

/** The check drawn before the label of the selected chip, so color never marks it alone. */
function CheckIcon({ color }: CheckIconProps) {
  return (
    <Svg
      width={CHECK_ICON.size}
      height={CHECK_ICON.size}
      viewBox="0 0 16 16"
      accessibilityElementsHidden
      importantForAccessibility="no"
    >
      <Path
        d="M3.5 8.5l3 3 6-7"
        fill="none"
        stroke={color}
        strokeWidth={CHECK_ICON.strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}

/** One pill-shaped option: raised and outlined, or selected with a strong border and a check. */
function Chip({ label, selected, onPress }: ChipProps) {
  const theme = useTheme();
  const { colors } = theme;
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
        {
          flexDirection: 'row',
          alignItems: 'center',
          gap: space[1],
          height: control.touch,
          paddingHorizontal: space[4],
          borderRadius: radius.full,
          borderWidth: borderWidth.hairline,
          borderColor: selected ? colors.text.primary : colors.border.control,
          // While pressed it takes the web version's hover color, like `Button`.
          backgroundColor: selected ? colors.surface.selected : pressed ? colors.surface.hover : colors.surface.raised,
        },
        focused && focusRing(theme),
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

/**
 * Single choice among a few short options, as wrapping chips (a radio group). Proposal from the
 * "Onboarding v1" screen design, pending adoption in the Lucas DS source project.
 */
export function ChoiceGroup<T extends string>({
  label,
  options,
  value,
  onChange,
  allowDeselect = false,
  error,
}: ChoiceGroupProps<T>) {
  return (
    <View style={{ flexDirection: 'column', gap: space[2] }}>
      <Text variant="body-sm" weight={500}>
        {label}
      </Text>
      <View
        accessibilityRole="radiogroup"
        accessibilityLabel={label}
        style={{ flexDirection: 'row', flexWrap: 'wrap', gap: space[2] }}
      >
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
