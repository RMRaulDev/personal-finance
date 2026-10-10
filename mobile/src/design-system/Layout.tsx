import { View, type ViewProps } from 'react-native';

import { useTheme } from './theme';
import { space } from './tokens';

/** Equivale a `r-root`: fondo de página. Va una vez, en la raíz de cada pantalla. */
export function Root({ style, ...rest }: ViewProps) {
  const { colors } = useTheme();
  return <View style={[{ flex: 1, backgroundColor: colors.surface.page }, style]} {...rest} />;
}

/** Equivale a `r-stack`: columna con gap 16. */
export function Stack({ style, ...rest }: ViewProps) {
  return <View style={[{ flexDirection: 'column', gap: space[4] }, style]} {...rest} />;
}

/** Equivale a `r-row`: fila con wrap, centrada en vertical y gap 12. */
export function Row({ style, ...rest }: ViewProps) {
  return <View style={[{ flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: space[3] }, style]} {...rest} />;
}
