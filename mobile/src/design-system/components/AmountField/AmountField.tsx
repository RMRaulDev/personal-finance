import { useId, useState } from 'react';
import { TextInput, View } from 'react-native';

import { focusRing } from '../../theme/focusRing';
import { Text } from '../Text/Text';
import { useTheme } from '../../theme/ThemeProvider';
import { borderWidth, control, fontFamily, radius, space, textStyles } from '../../tokens/colors';

/** `AmountField` props: label, the typed text, and its help or error. */
export interface AmountFieldProps {
  /** Visible label. Screen readers hear it followed by ", en pesos", because "$" and "MXN" are hidden. */
  label: string;
  /**
   * The text as shown, already cleaned and grouped by the caller (`formatAmountInput` in `lib/`).
   * The field never parses it: money stays in the caller's integer cents.
   */
  value: string;
  /** Receives the raw text the user typed or pasted. */
  onChangeText: (text: string) => void;
  /** Help text below the field. Hidden when there is an error. */
  help?: string;
  /** What happened and what to do, in one sentence. Marks the field (and its focus ring) as invalid. */
  error?: string;
  /** Focuses the field (and opens the decimal keyboard) when it mounts. */
  autoFocus?: boolean;
  /** False keeps the value from changing (e.g. while it is being sent). Defaults to true. */
  editable?: boolean;
}

/**
 * Amount field: a 64px container with "$", the amount in the amount text style, and "MXN", for
 * screens where typing an amount is the main task. Proposal from the "Onboarding v1" screen design,
 * pending adoption in the Lucas DS source project.
 */
export function AmountField({ label, value, onChangeText, help, error, autoFocus, editable = true }: AmountFieldProps) {
  const theme = useTheme();
  const { colors } = theme;
  const [focused, setFocused] = useState(false);
  const labelId = useId();
  const invalid = error !== undefined;
  const message = invalid ? error : help;
  const amount = textStyles.amount;

  return (
    <View style={{ flexDirection: 'column', gap: space[1] }}>
      <Text nativeID={labelId} variant="body-sm" weight={500}>
        {label}
      </Text>
      <View
        style={[
          {
            flexDirection: 'row',
            alignItems: 'center',
            gap: space[2],
            height: control.xl,
            paddingHorizontal: space[4],
            borderRadius: radius.md,
            borderWidth: borderWidth.hairline,
            borderColor: invalid ? colors.role.danger.fill : colors.border.control,
            backgroundColor: colors.surface.raised,
          },
          // The ring goes on the container (`:focus-within` in the web version), in danger when invalid.
          focused && focusRing(theme),
          focused && invalid && { outlineColor: colors.role.danger.fill },
        ]}
      >
        <Text
          variant="amount"
          weight={500}
          color="secondary"
          accessibilityElementsHidden
          importantForAccessibility="no"
        >
          $
        </Text>
        <TextInput
          value={value}
          onChangeText={onChangeText}
          autoFocus={autoFocus}
          editable={editable}
          keyboardType="decimal-pad"
          placeholder="0"
          placeholderTextColor={colors.text.secondary}
          accessibilityLabel={`${label}, en pesos`}
          aria-labelledby={labelId}
          accessibilityHint={message}
          aria-invalid={invalid}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          style={{
            flex: 1,
            minWidth: 0,
            height: '100%',
            padding: 0,
            fontFamily: fontFamily.sans[amount.weight],
            fontSize: amount.size,
            letterSpacing: amount.tracking * amount.size,
            fontVariant: ['tabular-nums'],
            color: colors.text.primary,
            // Android pads text vertically by default, which pushes a large single line off center.
            includeFontPadding: false,
            textAlignVertical: 'center',
          }}
        />
        <Text variant="body-sm" color="secondary" accessibilityElementsHidden importantForAccessibility="no">
          MXN
        </Text>
      </View>
      {message !== undefined && (
        <Text variant="body-sm" color={invalid ? 'danger' : 'secondary'}>
          {message}
        </Text>
      )}
    </View>
  );
}
