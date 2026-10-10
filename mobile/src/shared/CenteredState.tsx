import { StatusBar } from 'expo-status-bar';
import type { ReactNode } from 'react';

import { Root, space } from '../design-system';

/** Props of `CenteredState`. */
export interface CenteredStateProps {
  children: ReactNode;
}

/**
 * Full screen with its content centered, for the first load and for a load error, which the
 * designs do not cover. Internal to `shared/`: screens use `ScreenLoading` and `ScreenError`.
 */
export function CenteredState({ children }: CenteredStateProps) {
  return (
    <Root style={{ alignItems: 'center', justifyContent: 'center', padding: space[6] }}>
      <StatusBar style="auto" />
      {children}
    </Root>
  );
}
