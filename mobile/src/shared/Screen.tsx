import { StatusBar } from 'expo-status-bar';
import type { ReactNode } from 'react';
import { RefreshControl, ScrollView } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Root, space, useTheme } from '../design-system';

/** Props of `Screen`. */
export interface ScreenProps {
  /** Fixed content above the scrolling area (e.g. the date line); it does not scroll. */
  header?: ReactNode;
  /** True while a pull-to-refresh runs; shows the spinner. */
  refreshing: boolean;
  /** Called when the user pulls to refresh. */
  onRefresh: () => void;
  /** Floating content drawn over the screen, such as the toast. */
  overlay?: ReactNode;
  /** The screen's sections, spaced by the design's section gap. */
  children: ReactNode;
}

/**
 * Scaffolding of a screen with data: page background, top safe area, status bar, an optional fixed
 * header, the scrolling sections with pull-to-refresh, and an overlay slot for the toast.
 */
export function Screen({ header, refreshing, onRefresh, overlay, children }: ScreenProps) {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <Root style={{ paddingTop: insets.top }}>
      <StatusBar style="auto" />
      {header}
      <ScrollView
        contentContainerStyle={{
          gap: space[8],
          paddingTop: space[4],
          paddingHorizontal: space[5],
          paddingBottom: space[12] + insets.bottom,
        }}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={colors.role.accent.fill}
            colors={[colors.role.accent.fill]}
            // Android draws the spinner on a white disc by default, which breaks the dark theme.
            progressBackgroundColor={colors.surface.raised}
          />
        }
      >
        {children}
      </ScrollView>
      {overlay}
    </Root>
  );
}
