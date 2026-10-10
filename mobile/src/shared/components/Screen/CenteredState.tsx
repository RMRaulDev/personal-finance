import { StatusBar } from 'expo-status-bar';
import type { ReactNode } from 'react';

import { Root, useThemedStyles } from '../../../design-system';

import { createCenteredStateStyles } from './CenteredState.styles';

/** Props of `CenteredState`. */
export interface CenteredStateProps {
  children: ReactNode;
}

/**
 * Full screen with its content centered, for the first load and for a load error, which the
 * designs do not cover. Internal to the `Screen` unit: screens use `ScreenLoading` and `ScreenError`.
 */
export function CenteredState({ children }: CenteredStateProps) {
  const styles = useThemedStyles(createCenteredStateStyles);

  return (
    <Root style={styles.screen}>
      <StatusBar style="auto" />
      {children}
    </Root>
  );
}
