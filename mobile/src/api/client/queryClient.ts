import { focusManager, QueryClient } from '@tanstack/react-query';
import { AppState, Platform } from 'react-native';

import { isApiError } from './errors';

/** How many times a failed query is retried at most. */
const MAX_QUERY_RETRIES = 2;

/**
 * Query keys of every resource, defined in one place so a mutation can invalidate exactly the
 * data it changes. Each key starts with the resource name, so invalidating `obligations` also
 * invalidates every obligation and its occurrences.
 */
export const queryKeys = {
  dashboard: ['dashboard'],
  obligations: ['obligations'],
  /** One obligation: `['obligations', id]`. */
  obligation: (obligationId: string) => ['obligations', obligationId] as const,
  /** The occurrences of one obligation: `['obligations', id, 'occurrences']`. */
  obligationOccurrences: (obligationId: string) => ['obligations', obligationId, 'occurrences'] as const,
  operations: ['operations'],
  accounts: ['accounts'],
  categories: ['categories'],
} as const;

/**
 * Retries only failures that can go away by themselves: no connection or a 5xx. A 4xx or a
 * missing EXPO_PUBLIC_API_URL fails the same way every time, so retrying only delays the message.
 */
function shouldRetryQuery(failureCount: number, error: unknown): boolean {
  return failureCount < MAX_QUERY_RETRIES && isApiError(error) && (error.kind === 'network' || error.status >= 500);
}

/**
 * The app's single query client. Mutations never retry, because registrations and payments are not
 * idempotent (docs/backlog.md §3): a retry after a timeout could register the same payment twice.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: shouldRetryQuery },
    mutations: { retry: 0 },
  },
});

// React Native has no window focus event, so TanStack Query cannot tell when the app returns to the
// foreground. Without this, a dashboard left in the background overnight keeps yesterday's "today"
// (the server's horizon.from) until a manual pull-to-refresh.
focusManager.setEventListener((handleFocus) => {
  if (Platform.OS === 'web') {
    return undefined;
  }
  const subscription = AppState.addEventListener('change', (state) => handleFocus(state === 'active'));
  return () => subscription.remove();
});
