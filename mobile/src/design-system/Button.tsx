import { useState } from 'react';
import { Pressable, Text as RNText, type GestureResponderEvent, type PressableProps } from 'react-native';

import { focusRing } from './focus';
import { useTheme, type Theme } from './theme';
import { borderWidth, control, fontFamily, radius, space } from './tokens';

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

const sizes = {
  sm: { height: control.sm, paddingHorizontal: space[3], fontSize: 14 },
  md: { height: control.md, paddingHorizontal: space[4], fontSize: 14 },
  lg: { height: control.lg, paddingHorizontal: space[6], fontSize: 16 },
} as const;

function variantColors(theme: Theme, variant: ButtonVariant, pressed: boolean) {
  const { colors } = theme;
  switch (variant) {
    case 'primary':
      return {
        backgroundColor: pressed ? colors.role.accent.fillHover : colors.role.accent.fill,
        borderColor: 'transparent',
        color: colors.role.accent.on,
      };
    case 'secondary':
      return {
        backgroundColor: pressed ? colors.surface.hover : colors.surface.raised,
        borderColor: colors.border.control,
        color: colors.text.primary,
      };
    case 'ghost':
      return {
        backgroundColor: pressed ? colors.surface.hover : 'transparent',
        borderColor: 'transparent',
        color: colors.text.primary,
      };
  }
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
  const theme = useTheme();
  const [focused, setFocused] = useState(false);
  const sizing = sizes[size];

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
      style={({ pressed }) => {
        const tone = inactive
          ? { backgroundColor: theme.colors.surface.hover, borderColor: 'transparent' }
          : variantColors(theme, variant, pressed);
        return [
          {
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'center',
            alignSelf: fullWidth ? 'stretch' : 'flex-start',
            gap: space[2],
            height: sizing.height,
            paddingHorizontal: sizing.paddingHorizontal,
            borderRadius: radius.md,
            borderWidth: borderWidth.hairline,
            backgroundColor: tone.backgroundColor,
            borderColor: tone.borderColor,
          },
          focused && focusRing(theme),
        ];
      }}
      {...rest}
    >
      {({ pressed }) => (
        <RNText
          numberOfLines={1}
          style={{
            fontFamily: fontFamily.sans[500],
            fontSize: sizing.fontSize,
            color: inactive ? theme.colors.text.disabled : variantColors(theme, variant, pressed).color,
          }}
        >
          {children}
        </RNText>
      )}
    </Pressable>
  );
}
