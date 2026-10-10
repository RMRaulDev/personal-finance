import { Text as RNText, useWindowDimensions, type TextProps as RNTextProps } from 'react-native';

import { useTheme, type Theme } from '../../theme';
import { fontFamily, resolveFluid, textStyles, type FontWeight, type Role, type TextVariant } from '../../tokens';

/** Text color: one of the `--text-*` tokens or a role's `--text-{role}`. */
export type TextColor = 'primary' | 'secondary' | 'disabled' | Role;

/** Resolves a `TextColor` to its value in the active theme. */
export function resolveTextColor(theme: Theme, color: TextColor): string {
  switch (color) {
    case 'primary':
    case 'secondary':
    case 'disabled':
      return theme.colors.text[color];
    default:
      return theme.colors.role[color].text;
  }
}

/** `Text` props: the React Native props plus the design system type style and color. */
export interface TextProps extends RNTextProps {
  /** `display`, `title-1`, and `title-2` are fluid and page-level only; a component never uses them. */
  variant?: TextVariant;
  color?: TextColor;
  /** Fixed-width digits for tables, metrics, and changing values. */
  tabular?: boolean;
  /**
   * Font weight (400, 500, or 600; never 700). Defaults to the variant's weight. Use it instead of
   * setting `fontFamily` by hand, because in React Native each weight is a separate font family.
   */
  weight?: FontWeight;
}

/**
 * Design system text: applies the font, size, line height, tracking, and color of a type style.
 * Its style is computed while rendering instead of in a `.styles.ts`: the fluid sizes depend on
 * the window width, and the font, color, and digits on the props.
 */
export function Text({ variant = 'body', color = 'primary', tabular, weight, style, ...rest }: TextProps) {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const spec = textStyles[variant];
  const fontSize = typeof spec.size === 'number' ? spec.size : resolveFluid(spec.size, width);
  const tracking = 'tracking' in spec ? spec.tracking : undefined;

  return (
    <RNText
      style={[
        {
          fontFamily: fontFamily.sans[weight ?? spec.weight],
          fontSize,
          lineHeight: fontSize * spec.leading,
          letterSpacing: tracking === undefined ? undefined : tracking * fontSize,
          color: resolveTextColor(theme, color),
          fontVariant: tabular ? ['tabular-nums'] : undefined,
        },
        style,
      ]}
      {...rest}
    />
  );
}
