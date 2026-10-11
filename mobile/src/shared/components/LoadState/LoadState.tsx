import { StatusBar } from 'expo-status-bar';
import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ActivityBar, Button, Root, space, Text, useThemedStyles } from '../../../design-system';
import { BackHeader } from '../BackHeader';

import { createLoadStateStyles } from './LoadState.styles';

/** A load that failed: what to say and how to try again. */
export interface LoadFailure {
  /** What could not load, as a heading ("No pudimos cargar tus compromisos"). */
  title: string;
  /** What to do, in one or two sentences. */
  message: string;
  onRetry: () => void;
}

/** Props of `LoadState`. */
export interface LoadStateProps {
  /** Back button text ("‹ Inicio", "‹ Compromisos", "Cancelar"); the way out is always there. */
  backLabel: string;
  onBack: () => void;
  /** Page title shown under the back button, when the screen has a fixed one ("Compromisos"). */
  title?: string;
  /**
   * True once the load outlasts the delay (`useDelayedLoading`): shows the activity bar and
   * `loadingText`. Before that only the header and title show, so a quick load never flashes.
   */
  loading: boolean;
  /** What is loading, shown under the bar and read by screen readers ("Cargando compromisos…"). */
  loadingText: string;
  /** Set when the load failed and there is nothing to show: replaces the loading view. */
  failure?: LoadFailure;
}

/**
 * A screen with a back button (`BackHeader`) before it has data: the back button and title, then
 * either the delayed activity bar with its text, or the failure with "Reintentar" at the bottom (the
 * screen's only primary). Every text but "Reintentar" comes from the screen. Follows the
 * "Compromisos v1" design; screens without a back button use `ScreenLoading` and `ScreenError`.
 */
export function LoadState({ backLabel, onBack, title, loading, loadingText, failure }: LoadStateProps) {
  const styles = useThemedStyles(createLoadStateStyles);
  const insets = useSafeAreaInsets();

  return (
    <Root style={{ paddingTop: insets.top }}>
      <StatusBar style="auto" />
      <BackHeader label={backLabel} onPress={onBack} />
      {title !== undefined && (
        <View style={styles.title}>
          <Text variant="title-2" accessibilityRole="header">
            {title}
          </Text>
        </View>
      )}
      {failure !== undefined ? (
        <>
          <View accessibilityRole="alert" style={styles.error}>
            <Text variant="heading" accessibilityRole="header">
              {failure.title}
            </Text>
            <Text color="secondary">{failure.message}</Text>
          </View>
          <View style={[styles.footer, { paddingBottom: space[10] + insets.bottom }]}>
            <Button variant="primary" size="lg" fullWidth onPress={failure.onRetry}>
              Reintentar
            </Button>
          </View>
        </>
      ) : (
        <View accessibilityLiveRegion="polite" style={styles.loading}>
          {loading && (
            <>
              <ActivityBar accessibilityLabel={loadingText} />
              <Text color="secondary">{loadingText}</Text>
            </>
          )}
        </View>
      )}
    </Root>
  );
}
