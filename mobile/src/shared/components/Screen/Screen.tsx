import { StatusBar } from 'expo-status-bar';
import type { ReactNode, Ref } from 'react';
import { RefreshControl, ScrollView, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Root, space, useTheme, useThemedStyles } from '../../../design-system';

import { createScreenStyles } from './Screen.styles';

/** Props of `Screen`. */
export interface ScreenProps {
  /** Fixed content above the scrolling area (e.g. the date line); it does not scroll. */
  header?: ReactNode;
  /** True while a pull-to-refresh runs; shows the spinner. */
  refreshing?: boolean;
  /**
   * Called when the user pulls to refresh. Without it the screen has no pull-to-refresh, for a form
   * whose typed values must not be replaced.
   */
  onRefresh?: () => void;
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
  /** Ref to the scrolling area, for a screen that scrolls a section into view. */
  scrollRef?: Ref<ScrollView>;
}

/**
 * Scaffolding of a screen with data: page background, top safe area, status bar, an optional fixed
 * header, the scrolling sections with pull-to-refresh (when `onRefresh` is given), an overlay slot
 * for the toast, and an optional fixed footer.
 */
export function Screen({ header, refreshing = false, onRefresh, overlay, footer, children, scrollRef }: ScreenProps) {
  const { colors } = useTheme();
  const styles = useThemedStyles(createScreenStyles);
  const insets = useSafeAreaInsets();

  return (
    <Root style={{ paddingTop: insets.top }}>
      <StatusBar style="auto" />
      {header}
      <View style={styles.body}>
        <ScrollView
          ref={scrollRef}
          // A tap on a chip or button while the keyboard is open acts at once instead of only
          // closing the keyboard.
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={[
            styles.content,
            // Without a footer the content clears the bottom safe area itself.
            footer === undefined ? { paddingBottom: space[12] + insets.bottom } : styles.contentAboveFooter,
          ]}
          refreshControl={
            onRefresh === undefined ? undefined : (
              <RefreshControl
                refreshing={refreshing}
                onRefresh={onRefresh}
                tintColor={colors.role.accent.fill}
                colors={[colors.role.accent.fill]}
                // Android draws the spinner on a white disc by default, which breaks the dark theme.
                progressBackgroundColor={colors.surface.raised}
              />
            )
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
