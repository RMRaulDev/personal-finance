import { useEffect } from 'react';

import { useDelayedIndicator } from './useDelayedIndicator';

/**
 * Whether a screen's loading indicator should be on display while `pending` (e.g. a query's
 * `isPending`): only after 300 ms, and then for at least 600 ms (`useDelayedIndicator`). Before
 * the delay the screen shows nothing in its place, so a quick load never flashes the indicator.
 */
export function useDelayedLoading(pending: boolean): boolean {
  const { visible, begin, end, hide } = useDelayedIndicator();

  useEffect(() => {
    if (pending) {
      begin();
    } else {
      end(hide);
    }
  }, [pending, begin, end, hide]);

  return visible;
}
