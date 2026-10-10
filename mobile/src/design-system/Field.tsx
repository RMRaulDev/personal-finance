import { useId, useState } from 'react';
import { TextInput, View, type TextInputProps } from 'react-native';

import { focusRing } from './focus';
import { Text } from './Text';
import { useTheme } from './theme';
import { borderWidth, control, fontFamily, radius, space, textStyles } from './tokens';

export interface FieldProps extends Omit<TextInputProps, 'style' | 'placeholderTextColor'> {
  /** Etiqueta visible, siempre. */
  label: string;
  /** Texto de ayuda bajo el campo. Se oculta si hay error. */
  help?: string;
  /** Qué pasó y qué hacer, en una oración. Marca el campo como inválido. */
  error?: string;
  /** Un ejemplo real de entrada, nunca la etiqueta repetida. */
  placeholder?: string;
}

/** Equivale a `r-field` con `r-field__label`, `r-input` y `r-field__help` o `r-field__error`. */
export function Field({ label, help, error, onFocus, onBlur, ...rest }: FieldProps) {
  const theme = useTheme();
  const { colors } = theme;
  const [focused, setFocused] = useState(false);
  const labelId = useId();
  const invalid = error !== undefined;
  const message = invalid ? error : help;

  return (
    <View style={{ flexDirection: 'column', gap: space[1], maxWidth: 320 }}>
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
            height: control.md,
            paddingHorizontal: space[3],
            paddingVertical: 0,
            borderRadius: radius.md,
            borderWidth: borderWidth.hairline,
            borderColor: invalid ? colors.role.danger.fill : colors.border.control,
            backgroundColor: colors.surface.raised,
            color: colors.text.primary,
            fontFamily: fontFamily.sans[400],
            // Nunca menos de 16px: iOS hace zoom bajo 16px.
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
