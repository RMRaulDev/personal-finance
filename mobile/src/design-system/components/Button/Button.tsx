import { useState } from 'react';
import { Pressable, Text as RNText, type GestureResponderEvent, type PressableProps } from 'react-native';

import { useThemedStyles } from '../../theme';

import { createButtonStyles } from './Button.styles';

/** Button variant: `primary` (one per screen), `secondary` (default), or `ghost` (tertiary). */
export type ButtonVariant = 'primary' | 'secondary' | 'ghost';
/** Button size: 32, 40, or 48 high. */
export type ButtonSize = 'sm' | 'md' | 'lg';

/** `Button` props: the label, variant, size, and event handler. */
export interface ButtonProps extends Omit<PressableProps, 'children' | 'disabled' | 'style'> {
  /** Verb first, 1–3 words, sentence case. */
  children: string;
  /** `primary` only once per screen. Defaults to `secondary`. */
  variant?: ButtonVariant;
  size?: ButtonSize;
  /**
   * Avoid it. Equivalent to `aria-disabled="true"`: the button stays focusable and announced,
   * but does not fire `onPress`.
   */
  inactive?: boolean;
  /**
   * Stretches the button across its parent column (the design's `width: 100%`). In a row, wrap it
   * in a `flex: 1` view to give it the remaining width. Defaults to false: the button hugs its label.
   */
  fullWidth?: boolean;
}

/** Equivalent to `r-btn`: triggers an action. While pressed it takes the web version's hover color. */
export function Button({
  children,
  variant = 'secondary',
  size = 'md',
  inactive = false,
  fullWidth = false,
  onPress,
  onFocus,
  onBlur,
  ...rest
}: ButtonProps) {
  const styles = useThemedStyles(createButtonStyles);
  const [focused, setFocused] = useState(false);

  function handlePress(event: GestureResponderEvent) {
    if (!inactive) {
      onPress?.(event);
    }
  }

  return (
    <Pressable
      accessibilityRole="button"
      aria-disabled={inactive}
      onPress={handlePress}
      onFocus={(event) => {
        setFocused(true);
        onFocus?.(event);
      }}
      onBlur={(event) => {
        setFocused(false);
        onBlur?.(event);
      }}
      style={({ pressed }) => [
        styles.button,
        fullWidth && styles.fullWidth,
        styles[size],
        inactive ? styles.inactive : [styles[variant], pressed && styles[`${variant}Pressed` as const]],
        focused && styles.focused,
      ]}
      {...rest}
    >
      <RNText
        numberOfLines={1}
        style={[
          styles.label,
          styles[`${size}Label` as const],
          inactive ? styles.inactiveLabel : styles[`${variant}Label` as const],
        ]}
      >
        {children}
      </RNText>
    </Pressable>
  );
}
