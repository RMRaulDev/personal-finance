import { View, type ViewProps } from 'react-native';

import { useThemedStyles } from '../../theme';

import { createLayoutStyles } from './Layout.styles';

/** Equivalent to `r-root`: page background. Used once, at the root of each screen. */
export function Root({ style, ...rest }: ViewProps) {
  const styles = useThemedStyles(createLayoutStyles);
  return <View style={[styles.root, style]} {...rest} />;
}
