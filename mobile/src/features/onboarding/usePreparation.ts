import { useCallback, useEffect, useRef, useState } from 'react';

import { preparationDelayMs, preparationMinimumMs } from './designSpec';

/** What `usePreparation` returns. */
export interface Preparation {
  /** True while the preparation screen is on display. */
  visible: boolean;
  /** Call when the request starts: the screen appears only if the request outlasts the delay. */
  begin: () => void;
  /**
   * Call when the request settles. Runs `then` right away if the screen never appeared, or once it
   * has been visible for the minimum time. It does not hide the screen: on success the caller
   * navigates away from it, and on failure it calls `hide`.
   */
  end: (then: () => void) => void;
  /** Hides the screen. */
  hide: () => void;
}

/**
 * Timing of the setup's preparation screen, as the design asks: it is not shown for a quick
 * answer (under 300 ms), and once shown it stays at least 600 ms so it never flickers.
 */
export function usePreparation(): Preparation {
  const [visible, setVisible] = useState(false);
  const showTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const endTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  // When the screen appeared, or null while it is not shown.
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
    // A retry from the error screen starts over: it may answer quickly and never show the screen.
    shownAt.current = null;
    showTimer.current = setTimeout(() => {
      shownAt.current = Date.now();
      setVisible(true);
    }, preparationDelayMs);
  }, []);

  const end = useCallback((then: () => void) => {
    clearTimeout(showTimer.current);
    const since = shownAt.current;
    if (since === null) {
      then();
      return;
    }
    const remaining = Math.max(0, preparationMinimumMs - (Date.now() - since));
    endTimer.current = setTimeout(then, remaining);
  }, []);

  const hide = useCallback(() => {
    shownAt.current = null;
    setVisible(false);
  }, []);

  return { visible, begin, end, hide };
}
