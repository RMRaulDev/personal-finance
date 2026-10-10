import { View, type ViewProps } from 'react-native';

import { useThemedStyles } from '../../theme';

import { createLayoutStyles } from './Layout.styles';

/** Equivalent to `r-row`: a wrapping row, vertically centered, with a 12 gap. */
export function Row({ style, ...rest }: ViewProps) {
  const styles = useThemedStyles(createLayoutStyles);
  return <View style={[styles.row, style]} {...rest} />;
}
