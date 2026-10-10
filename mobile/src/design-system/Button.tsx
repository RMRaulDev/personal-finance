import { useState } from 'react';
import { Pressable, Text as RNText, type GestureResponderEvent, type PressableProps } from 'react-native';

import { focusRing } from './focus';
import { useTheme, type Theme } from './theme';
import { borderWidth, control, fontFamily, radius, space } from './tokens';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost';
export type ButtonSize = 'sm' | 'md' | 'lg';

export interface ButtonProps extends Omit<PressableProps, 'children' | 'disabled' | 'style'> {
  /** Verbo primero, 1–3 palabras, sentence case. */
  children: string;
  /** `primary` solo una vez por vista. Por defecto `secondary`. */
  variant?: ButtonVariant;
  size?: ButtonSize;
  /**
   * Evítalo. Equivale a `aria-disabled="true"`: el botón sigue siendo enfocable y anunciado,
   * pero no dispara `onPress`.
   */
  inactive?: boolean;
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

export function Button({
  children,
  variant = 'secondary',
  size = 'md',
  inactive = false,
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
            alignSelf: 'flex-start',
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
