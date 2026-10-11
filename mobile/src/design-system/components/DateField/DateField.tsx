import { useState } from 'react';
import { Pressable, View } from 'react-native';

import { useThemedStyles } from '../../theme';
import { ChoiceGroup, type ChoiceOption } from '../ChoiceGroup';
import { Text } from '../Text';

import { CalendarIcon } from './CalendarIcon';
import { createDateFieldStyles } from './DateField.styles';

/** `DateField` props. */
export interface DateFieldProps {
  /** Label of the field; also its accessible name. */
  label: string;
  /** Hides the label, for a field under a control that already labels it. Defaults to false. */
  labelHidden?: boolean;
  /** The chosen date as text, with its weekday (e.g. "Jueves 15 oct"). Undefined shows `placeholder`. */
  text?: string;
  /** Shown in secondary text while no date is chosen (e.g. "Elige una fecha"). */
  placeholder: string;
  /** Opens the month sheet (`DatePickerSheet`). */
  onPress: () => void;
  /** Quick dates shown as chips under the field, so the common dates need no calendar. */
  quickDates?: ChoiceOption<string>[];
  /** The chosen ISO date, to mark its quick date; null when none is chosen. */
  value?: string | null;
  /** Receives the ISO date of the quick date the user picks. */
  onQuickPick?: (iso: string) => void;
  /** Help text below the field. Hidden when there is an error. */
  help?: string;
  /** What happened and what to do, in one sentence. Marks the field as invalid. */
  error?: string;
}

/**
 * Date field: shows the chosen date with its weekday, offers quick dates as chips, and opens a month
 * sheet for any other date. Same height and borders as `Field`'s large input. Part of the date
 * picker proposal ("Selector de fecha") from the "Compromisos v1" screen design, pending adoption
 * in the Lucas DS source project.
 */
export function DateField({
  label,
  labelHidden = false,
  text,
  placeholder,
  onPress,
  quickDates,
  value = null,
  onQuickPick,
  help,
  error,
}: DateFieldProps) {
  const styles = useThemedStyles(createDateFieldStyles);
  const [focused, setFocused] = useState(false);
  const invalid = error !== undefined;
  const message = invalid ? error : help;

  return (
    <View style={styles.field}>
      {!labelHidden && (
        <Text variant="body-sm" weight={500}>
          {label}
        </Text>
      )}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${label}: ${text ?? placeholder}`}
        accessibilityHint={message}
        aria-invalid={invalid}
        onPress={onPress}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        style={({ pressed }) => [
          styles.box,
          pressed && styles.pressed,
          invalid && styles.invalid,
          focused && styles.focused,
        ]}
      >
        <Text color={text === undefined ? 'secondary' : 'primary'} numberOfLines={1} style={styles.text}>
          {text ?? placeholder}
        </Text>
        <CalendarIcon />
      </Pressable>
      {quickDates !== undefined && quickDates.length > 0 && (
        <ChoiceGroup
          label="Fechas rápidas"
          labelHidden
          options={quickDates}
          value={value}
          onChange={(picked) => {
            if (picked !== null) {
              onQuickPick?.(picked);
            }
          }}
        />
      )}
      {message !== undefined && (
        <Text variant="body-sm" color={invalid ? 'danger' : 'secondary'}>
          {message}
        </Text>
      )}
    </View>
  );
}
