import type { ReactNode } from 'react';
import { View, type ViewProps } from 'react-native';

import { Text } from './Text';
import { useTheme } from './theme';
import { borderWidth, radius, space } from './tokens';

export interface CardProps extends ViewProps {
  title: string;
  /** Cuerpo en `body` con `text-secondary`. Un string se envuelve; cualquier otro nodo se pinta tal cual. */
  children?: ReactNode;
  /** Botones `secondary` o `ghost` en tamaño `sm`; nunca `primary`. */
  actions?: ReactNode;
  /** Padding 16 en lugar de 24, para layouts densos. */
  dense?: boolean;
}

/** Equivale a `r-card`: se delimita con borde, nunca con sombra. */
export function Card({ title, children, actions, dense = false, style, ...rest }: CardProps) {
  const { colors } = useTheme();

  return (
    <View
      style={[
        {
          backgroundColor: colors.surface.raised,
          borderWidth: borderWidth.hairline,
          borderColor: colors.border.subtle,
          borderRadius: radius.lg,
          padding: dense ? space[4] : space[6],
          maxWidth: 360,
        },
        style,
      ]}
      {...rest}
    >
      <Text variant="heading" accessibilityRole="header">
        {title}
      </Text>
      {children !== undefined && (
        <View style={{ marginTop: space[2] }}>
          {typeof children === 'string' ? (
            <Text variant="body" color="secondary">
              {children}
            </Text>
          ) : (
            children
          )}
        </View>
      )}
      {actions !== undefined && <View style={{ marginTop: space[4], flexDirection: 'row', gap: space[2] }}>{actions}</View>}
    </View>
  );
}
