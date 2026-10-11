import { useCallback, useEffect, useRef } from 'react';

/**
 * Returns a function that tells whether the component is still mounted. A screen checks it before
 * navigating when a mutation settles, because the user may have left the screen meanwhile (e.g.
 * with Android's Back), and navigating back again would leave the screen they went to.
 */
export function useIsMounted(): () => boolean {
  const mounted = useRef(false);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  return useCallback(() => mounted.current, []);
}
