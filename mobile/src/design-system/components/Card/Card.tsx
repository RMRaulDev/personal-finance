import type { ReactNode } from 'react';
import { View, type ViewProps } from 'react-native';

import { useThemedStyles } from '../../theme';
import { Text } from '../Text';

import { createCardStyles } from './Card.styles';

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
  const styles = useThemedStyles(createCardStyles);

  return (
    <View style={[styles.card, dense && styles.dense, style]} {...rest}>
      <Text variant="heading" accessibilityRole="header">
        {title}
      </Text>
      {children !== undefined && (
        <View style={styles.body}>
          {typeof children === 'string' ? (
            <Text variant="body" color="secondary">
              {children}
            </Text>
          ) : (
            children
          )}
        </View>
      )}
      {actions !== undefined && <View style={styles.actions}>{actions}</View>}
    </View>
  );
}
