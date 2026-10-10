import { Children, Fragment, isValidElement, type ReactNode } from 'react';
import { View } from 'react-native';

import { useThemedStyles } from '../../../design-system';

import { createListGroupStyles } from './ListGroup.styles';

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
  const styles = useThemedStyles(createListGroupStyles);
  const items = Children.toArray(children);

  return (
    <View style={[styles.group, padded && styles.padded]}>
      {items.map((child, index) => (
        // `Children.toArray` keys every element, so the row's own key is kept.
        <Fragment key={isValidElement(child) && child.key !== null ? child.key : index}>
          {index > 0 && <View style={styles.divider} />}
          {child}
        </Fragment>
      ))}
    </View>
  );
}
