import { View, type ViewProps } from 'react-native';

import { useThemedStyles } from '../../theme';

import { createLayoutStyles } from './Layout.styles';

/** Equivalent to `r-stack`: a column with a 16 gap. */
export function Stack({ style, ...rest }: ViewProps) {
  const styles = useThemedStyles(createLayoutStyles);
  return <View style={[styles.stack, style]} {...rest} />;
}
