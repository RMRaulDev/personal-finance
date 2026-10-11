import { QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { queryClient } from '../api';
import { DesignSystemProvider } from '../design-system';
import { ToastProvider } from '../shared';

/**
 * Root layout: mounts the app-wide providers (the app's single toast among them, so a toast survives
 * navigation) and a header-less stack for the screens. The entry route (`index`) sends the user to
 * `onboarding` or `dashboard`.
 */
export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <DesignSystemProvider>
          <ToastProvider>
            <Stack screenOptions={{ headerShown: false }}>
              {/* The design fades the onboarding into the dashboard in 200 ms. */}
              <Stack.Screen name="dashboard" options={{ animation: 'fade', animationDuration: 200 }} />
            </Stack>
          </ToastProvider>
        </DesignSystemProvider>
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}
