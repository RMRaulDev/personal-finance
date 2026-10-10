import { Text as RNText, useWindowDimensions, type TextProps as RNTextProps } from 'react-native';

import { useTheme, type Theme } from './theme';
import { codeScale, fontFamily, resolveFluid, textStyles, type Role, type TextVariant } from './tokens';

export type TextColor = 'primary' | 'secondary' | 'disabled' | Role;

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

export interface TextProps extends RNTextProps {
  /** `display`, `title-1` y `title-2` son fluidos y solo de página; un componente nunca los usa. */
  variant?: TextVariant;
  color?: TextColor;
  /** Cifras de ancho fijo para tablas, métricas y valores que cambian. */
  tabular?: boolean;
}

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

export interface CodeProps extends RNTextProps {
  /** Tamaño del texto que lo rodea; el código mide 0.875 de él. */
  parentSize?: number;
}

/** Texto monoespaciado para código y datos, pensado para ir anidado dentro de `Text`. */
export function Code({ parentSize = textStyles.body.size, style, ...rest }: CodeProps) {
  return <RNText style={[{ fontFamily: fontFamily.mono, fontSize: parentSize * codeScale }, style]} {...rest} />;
}
