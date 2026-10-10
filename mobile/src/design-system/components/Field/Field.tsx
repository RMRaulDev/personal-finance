import { useId, useState } from 'react';
import { TextInput, View, type TextInputProps } from 'react-native';

import { focusRing } from '../../theme/focusRing';
import { Text } from '../Text/Text';
import { useTheme } from '../../theme/ThemeProvider';
import { borderWidth, control, fontFamily, radius, space, textStyles } from '../../tokens/colors';

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

/** Field width cap of `r-field` in the web version, unless `fullWidth` lifts it. */
const FIELD_MAX_WIDTH = 320;

/** Equivalent to `r-field` with `r-field__label`, `r-input`, and `r-field__help` or `r-field__error`. */
export function Field({ label, help, error, size = 'md', fullWidth = false, onFocus, onBlur, ...rest }: FieldProps) {
  const theme = useTheme();
  const { colors } = theme;
  const [focused, setFocused] = useState(false);
  const labelId = useId();
  const invalid = error !== undefined;
  const message = invalid ? error : help;

  return (
    <View style={{ flexDirection: 'column', gap: space[1], maxWidth: fullWidth ? undefined : FIELD_MAX_WIDTH }}>
      <Text nativeID={labelId} variant="body-sm" style={{ fontFamily: fontFamily.sans[500] }}>
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
        style={[
          {
            height: control[size],
            paddingHorizontal: space[3],
            paddingVertical: 0,
            borderRadius: radius.md,
            borderWidth: borderWidth.hairline,
            borderColor: invalid ? colors.role.danger.fill : colors.border.control,
            backgroundColor: colors.surface.raised,
            color: colors.text.primary,
            fontFamily: fontFamily.sans[400],
            // Never below 16px: iOS zooms in on inputs smaller than 16px.
            fontSize: textStyles.body.size,
          },
          focused && focusRing(theme),
        ]}
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
