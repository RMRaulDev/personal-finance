import type { ReactNode } from 'react';
import { View, type ViewProps } from 'react-native';

import { Text } from './Text';
import { useTheme } from './theme';
import { borderWidth, radius, space } from './tokens';

/** `Card` props: title, body, and optional actions. */
export interface CardProps extends ViewProps {
  title: string;
  /** Body in `body` with `text-secondary`. A string is wrapped; any other node renders as is. */
  children?: ReactNode;
  /** `secondary` or `ghost` buttons with size `sm`; never `primary`. */
  actions?: ReactNode;
  /** Padding 16 instead of 24, for dense layouts. */
  dense?: boolean;
}

/** Equivalent to `r-card`: delimited by a border, never by a shadow. */
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
      {actions !== undefined && (
        <View style={{ marginTop: space[4], flexDirection: 'row', gap: space[2] }}>{actions}</View>
      )}
    </View>
  );
}
