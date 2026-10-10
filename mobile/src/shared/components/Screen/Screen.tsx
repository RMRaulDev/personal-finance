import { StatusBar } from 'expo-status-bar';
import type { ReactNode } from 'react';
import { RefreshControl, ScrollView, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Root, space, useTheme, useThemedStyles } from '../../../design-system';

import { createScreenStyles } from './Screen.styles';

/** Props of `Screen`. */
export interface ScreenProps {
  /** Fixed content above the scrolling area (e.g. the date line); it does not scroll. */
  header?: ReactNode;
  /** True while a pull-to-refresh runs; shows the spinner. */
  refreshing: boolean;
  /** Called when the user pulls to refresh. */
  onRefresh: () => void;
  /**
   * Floating content drawn over the scrolling area, such as the toast. With a `footer`, it floats
   * above the footer, so a toast never covers the footer's actions.
   */
  overlay?: ReactNode;
  /**
   * Fixed content below the scrolling area (e.g. a bottom action bar). It does not scroll and pads
   * the bottom safe area itself.
   */
  footer?: ReactNode;
  /** The screen's sections, spaced by the design's section gap. */
  children: ReactNode;
}

/**
 * Scaffolding of a screen with data: page background, top safe area, status bar, an optional fixed
 * header, the scrolling sections with pull-to-refresh, an overlay slot for the toast, and an
 * optional fixed footer.
 */
export function Screen({ header, refreshing, onRefresh, overlay, footer, children }: ScreenProps) {
  const { colors } = useTheme();
  const styles = useThemedStyles(createScreenStyles);
  const insets = useSafeAreaInsets();

  return (
    <Root style={{ paddingTop: insets.top }}>
      <StatusBar style="auto" />
      {header}
      <View style={styles.body}>
        <ScrollView
          contentContainerStyle={[
            styles.content,
            // Without a footer the content clears the bottom safe area itself.
            footer === undefined ? { paddingBottom: space[12] + insets.bottom } : styles.contentAboveFooter,
          ]}
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
      </View>
      {footer}
    </Root>
  );
}
