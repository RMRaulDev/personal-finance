import { useCallback, useRef, useState } from 'react';

/** What `useMutationLock` returns. */
export interface MutationLock {
  /** True while a mutation started through `run` has not settled; submit buttons render inactive. */
  locked: boolean;
  /**
   * Starts a mutation unless another one is still running, in which case the call is ignored.
   * `start` receives `release`, which the caller passes to the mutation's `onSettled`.
   */
  run: (start: (release: () => void) => void) => void;
}

/**
 * Lets a screen run one mutation at a time. The guard is a ref, checked synchronously, because a
 * double tap fires twice before `isPending` (or any state) reaches the render, and payments are not
 * idempotent. Running one at a time also guarantees TanStack Query runs the per-call `mutate`
 * callbacks, which it only runs for the latest `mutate` call, so no success loses its toast.
 */
export function useMutationLock(): MutationLock {
  const lockedRef = useRef(false);
  const [locked, setLocked] = useState(false);

  const release = useCallback(() => {
    lockedRef.current = false;
    setLocked(false);
  }, []);

  const run = useCallback(
    (start: (release: () => void) => void) => {
      if (lockedRef.current) {
        return;
      }
      lockedRef.current = true;
      setLocked(true);
      start(release);
    },
    [release],
  );

  return { locked, run };
}
