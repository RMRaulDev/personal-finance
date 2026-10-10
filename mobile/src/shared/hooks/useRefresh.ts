import { useState } from 'react';

/** The part of a TanStack Query refetch result that `useRefresh` reads. */
export interface RefetchResult {
  isError: boolean;
  error: unknown;
}

/** What `useRefresh` returns. */
export interface Refresh {
  /** True while the refetchers run; drives the pull-to-refresh spinner. */
  refreshing: boolean;
  /** Runs every refetcher. Safe to call from `onRefresh`: it never throws. */
  refresh: () => void;
}

/**
 * Pull-to-refresh state for a screen: `refresh` runs every refetcher at once (usually the
 * `refetch` of each query the screen shows), waits for all of them, and reports the first failed
 * result, in the order given, through `onError`. The queries keep their last data on failure.
 */
export function useRefresh(refetchers: (() => Promise<RefetchResult>)[], onError: (error: unknown) => void): Refresh {
  const [refreshing, setRefreshing] = useState(false);

  /** Runs the refetchers and reports the first failure. */
  async function run() {
    setRefreshing(true);
    try {
      const results = await Promise.all(refetchers.map((refetch) => refetch()));
      const failed = results.find((result) => result.isError);
      if (failed) {
        onError(failed.error);
      }
    } catch (error) {
      // TanStack's refetch does not throw by default; any other refetcher is reported the same way.
      onError(error);
    } finally {
      setRefreshing(false);
    }
  }

  return { refreshing, refresh: () => void run() };
}
