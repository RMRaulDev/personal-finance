import { useId, useState } from 'react';
import { TextInput, View, type TextInputProps } from 'react-native';

import { useTheme, useThemedStyles } from '../../theme';
import { Text } from '../Text';

import { createFieldStyles } from './Field.styles';

/** `Field` props: the `TextInput` props plus label, help, and error. */
export interface FieldProps extends Omit<TextInputProps, 'style' | 'placeholderTextColor'> {
  /** Visible label, always. */
  label: string;
  /** Help text below the field. Hidden when there is an error. */
  help?: string;
  /** What happened and what to do, in one sentence. Marks the field as invalid. */
  error?: string;
  /** A real example input, never the label repeated. */
  placeholder?: string;
  /** Input height: `md` (40, default) or `lg` (48, mobile forms where the field is the screen's focus). */
  size?: 'md' | 'lg';
  /** Fills the parent's width instead of stopping at the 320px default, as some screen designs ask. */
  fullWidth?: boolean;
}

/** Equivalent to `r-field` with `r-field__label`, `r-input`, and `r-field__help` or `r-field__error`. */
export function Field({ label, help, error, size = 'md', fullWidth = false, onFocus, onBlur, ...rest }: FieldProps) {
  const { colors } = useTheme();
  const styles = useThemedStyles(createFieldStyles);
  const [focused, setFocused] = useState(false);
  const labelId = useId();
  const invalid = error !== undefined;
  const message = invalid ? error : help;

  return (
    <View style={[styles.field, !fullWidth && styles.capped]}>
      <Text nativeID={labelId} variant="body-sm" style={styles.label}>
        {label}
      </Text>
      <TextInput
        accessibilityLabel={label}
        aria-labelledby={labelId}
        accessibilityHint={message}
        aria-invalid={invalid}
        placeholderTextColor={colors.text.secondary}
        onFocus={(event) => {
          setFocused(true);
          onFocus?.(event);
        }}
        onBlur={(event) => {
          setFocused(false);
          onBlur?.(event);
        }}
        style={[styles.input, styles[size], invalid && styles.invalid, focused && styles.focused]}
        {...rest}
      />
      {message !== undefined && (
        <Text variant="body-sm" color={invalid ? 'danger' : 'secondary'}>
          {message}
        </Text>
      )}
    </View>
  );
}
