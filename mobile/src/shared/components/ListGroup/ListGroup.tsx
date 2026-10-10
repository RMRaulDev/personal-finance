import { Children, Fragment, isValidElement, type ReactNode } from 'react';
import { View } from 'react-native';

import { borderWidth, radius, space, useTheme } from '../../../design-system';

/** Props of `ListGroup`. */
export interface ListGroupProps {
  /** The rows. A 1px divider is drawn between them, so rows draw no borders of their own. */
  children: ReactNode;
  /**
   * Pads the group by `space[5]`, for a group that holds a single message instead of rows (e.g. an
   * empty-state invite). Default false: rows bring their own padding.
   */
  padded?: boolean;
}

/**
 * Grouped list from the screen designs: a raised surface delimited by a subtle border (never a
 * shadow), with a hairline divider between its rows.
 */
export function ListGroup({ children, padded = false }: ListGroupProps) {
  const { colors } = useTheme();
  const items = Children.toArray(children);

  return (
    <View
      style={{
        backgroundColor: colors.surface.raised,
        borderWidth: borderWidth.hairline,
        borderColor: colors.border.subtle,
        borderRadius: radius.lg,
        overflow: 'hidden',
        padding: padded ? space[5] : undefined,
      }}
    >
      {items.map((child, index) => (
        // `Children.toArray` keys every element, so the row's own key is kept.
        <Fragment key={isValidElement(child) && child.key !== null ? child.key : index}>
          {index > 0 && <View style={{ height: borderWidth.hairline, backgroundColor: colors.border.subtle }} />}
          {child}
        </Fragment>
      ))}
    </View>
  );
}
