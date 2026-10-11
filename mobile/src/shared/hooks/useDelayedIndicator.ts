import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * A wait indicator only appears when the work takes longer than this, in milliseconds, so a quick
 * answer never flashes it ("Onboarding v1" and "Compromisos v1" designs).
 */
const INDICATOR_DELAY_MS = 300;

/** Once shown, the indicator stays at least this long so it does not flicker, in ms. */
const INDICATOR_MINIMUM_MS = 600;

/** What `useDelayedIndicator` returns. */
export interface DelayedIndicator {
  /** True while the indicator is on display. */
  visible: boolean;
  /** Call when the work starts: the indicator appears only if the work outlasts the delay. */
  begin: () => void;
  /**
   * Call when the work settles. Runs `then` right away if the indicator never appeared, or once it
   * has been visible for the minimum time. It does not hide the indicator: the caller decides (e.g.
   * the onboarding navigates away on success and calls `hide` on failure).
   */
  end: (then: () => void) => void;
  /** Hides the indicator. */
  hide: () => void;
}

/**
 * Timing of a wait indicator, as the designs ask: it is not shown for a quick answer (under 300
 * ms), and once shown it stays at least 600 ms so it never flickers. Used by the onboarding's
 * preparation screen and, through `useDelayedLoading`, by screens that load their data.
 */
export function useDelayedIndicator(): DelayedIndicator {
  const [visible, setVisible] = useState(false);
  const showTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const endTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  // When the indicator appeared, or null while it is not shown.
  const shownAt = useRef<number | null>(null);

  useEffect(
    () => () => {
      clearTimeout(showTimer.current);
      clearTimeout(endTimer.current);
    },
    [],
  );

  const begin = useCallback(() => {
    clearTimeout(showTimer.current);
    clearTimeout(endTimer.current);
    // A retry starts over: it may answer quickly and never show the indicator.
    shownAt.current = null;
    showTimer.current = setTimeout(() => {
      shownAt.current = Date.now();
      setVisible(true);
    }, INDICATOR_DELAY_MS);
  }, []);

  const end = useCallback((then: () => void) => {
    clearTimeout(showTimer.current);
    const since = shownAt.current;
    if (since === null) {
      then();
      return;
    }
    const remaining = Math.max(0, INDICATOR_MINIMUM_MS - (Date.now() - since));
    endTimer.current = setTimeout(then, remaining);
  }, []);

  const hide = useCallback(() => {
    shownAt.current = null;
    setVisible(false);
  }, []);

  return { visible, begin, end, hide };
}
