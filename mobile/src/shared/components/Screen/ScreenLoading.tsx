import { ActivityIndicator } from 'react-native';

import { useTheme } from '../../../design-system';

import { CenteredState } from './CenteredState';

/**
 * A screen's first load, before it has any data. The design system has no loading state yet, so
 * it is the platform spinner in the accent color.
 */
export function ScreenLoading() {
  const { colors } = useTheme();
  return (
    <CenteredState>
      <ActivityIndicator color={colors.role.accent.fill} accessibilityLabel="Cargando" />
    </CenteredState>
  );
}
