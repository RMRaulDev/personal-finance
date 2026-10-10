import { Text as RNText, useWindowDimensions, type TextProps as RNTextProps } from 'react-native';

import { useTheme, type Theme } from './theme';
import { codeScale, fontFamily, resolveFluid, textStyles, type Role, type TextVariant } from './tokens';

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
}

/** Design system text: applies the font, size, line height, tracking, and color of a type style. */
export function Text({ variant = 'body', color = 'primary', tabular, style, ...rest }: TextProps) {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const spec = textStyles[variant];
  const fontSize = typeof spec.size === 'number' ? spec.size : resolveFluid(spec.size, width);
  const tracking = 'tracking' in spec ? spec.tracking : undefined;

  return (
    <RNText
      style={[
        {
          fontFamily: fontFamily.sans[spec.weight],
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

/** `Code` props: the React Native props plus the size of the surrounding text. */
export interface CodeProps extends RNTextProps {
  /** Size of the surrounding text; code is 0.875 of it. */
  parentSize?: number;
}

/** Monospaced text for code and data, meant to be nested inside `Text`. */
export function Code({ parentSize = textStyles.body.size, style, ...rest }: CodeProps) {
  return <RNText style={[{ fontFamily: fontFamily.mono, fontSize: parentSize * codeScale }, style]} {...rest} />;
}
