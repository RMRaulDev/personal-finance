import { useEffect, useState } from 'react';
import { AccessibilityInfo, Animated, Easing, View } from 'react-native';

import { useThemedStyles } from '../../theme';

import { BAR_WIDTH, createActivityBarStyles } from './ActivityBar.styles';

/** `ActivityBar` props. */
export interface ActivityBarProps {
  /** What is in progress, for screen readers (e.g. "Preparando tu cuenta"). */
  accessibilityLabel: string;
}

/** One sweep of the bar, in milliseconds. */
const CYCLE_MS = 1400;
/** Start of the sweep, as a multiple of the bar's width (−110%): fully left of the track. */
const SWEEP_FROM = -1.1;
/** End of the sweep, as a multiple of the bar's width (260%): fully right of the track. */
const SWEEP_TO = 2.6;

/**
 * Whether the system asks to reduce motion, kept up to date. Starts false and settles after the
 * first read, so the bar may start moving for a frame before it stops.
 */
function useReduceMotion(): boolean {
  const [reduceMotion, setReduceMotion] = useState(false);

  useEffect(() => {
    let active = true;
    void AccessibilityInfo.isReduceMotionEnabled().then((enabled) => {
      if (active) {
        setReduceMotion(enabled);
      }
    });
    const subscription = AccessibilityInfo.addEventListener('reduceMotionChanged', setReduceMotion);
    return () => {
      active = false;
      subscription.remove();
    };
  }, []);

  return reduceMotion;
}

/**
 * Indeterminate activity indicator: a short accent bar sweeping along a thin track, for a wait
 * the user cannot act on (e.g. preparing an account). With reduced motion the bar stays still. It
 * does not replace a button's loading state, which the design system still lists as pending.
 * Named `ActivityBar` so it does not clash with React Native's `ActivityIndicator`. Proposal from
 * the "Onboarding v1" screen design, pending adoption in the Lucas DS source project.
 */
export function ActivityBar({ accessibilityLabel }: ActivityBarProps) {
  const styles = useThemedStyles(createActivityBarStyles);
  const reduceMotion = useReduceMotion();
  // State, not a ref: the value is created once and read while rendering (for `interpolate`).
  const [progress] = useState(() => new Animated.Value(0));

  useEffect(() => {
    if (reduceMotion) {
      progress.setValue(0);
      return undefined;
    }
    const loop = Animated.loop(
      Animated.timing(progress, {
        toValue: 1,
        duration: CYCLE_MS,
        easing: Easing.inOut(Easing.ease),
        useNativeDriver: true,
      }),
    );
    loop.start();
    return () => loop.stop();
  }, [progress, reduceMotion]);

  // With reduced motion the bar rests at the start of the track instead of sweeping.
  const translateX = reduceMotion
    ? 0
    : progress.interpolate({ inputRange: [0, 1], outputRange: [SWEEP_FROM * BAR_WIDTH, SWEEP_TO * BAR_WIDTH] });

  return (
    <View accessibilityRole="progressbar" accessibilityLabel={accessibilityLabel} style={styles.track}>
      {/* The sweep is animated, so its transform is combined with the static bar style. */}
      <Animated.View style={[styles.bar, { transform: [{ translateX }] }]} />
    </View>
  );
}
