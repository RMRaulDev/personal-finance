import { View, type ViewProps } from 'react-native';

import { useTheme } from './theme';
import { space } from './tokens';

/** Equivalent to `r-root`: page background. Used once, at the root of each screen. */
export function Root({ style, ...rest }: ViewProps) {
  const { colors } = useTheme();
  return <View style={[{ flex: 1, backgroundColor: colors.surface.page }, style]} {...rest} />;
}

/** Equivalent to `r-stack`: a column with a 16 gap. */
export function Stack({ style, ...rest }: ViewProps) {
  return <View style={[{ flexDirection: 'column', gap: space[4] }, style]} {...rest} />;
}

/** Equivalent to `r-row`: a wrapping row, vertically centered, with a 12 gap. */
export function Row({ style, ...rest }: ViewProps) {
  return (
    <View style={[{ flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: space[3] }, style]} {...rest} />
  );
}
