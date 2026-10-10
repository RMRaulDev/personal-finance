import { QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { queryClient } from '../api';
import { DesignSystemProvider } from '../design-system';

/** Root layout: mounts the app-wide providers and a header-less stack for the screens. */
export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <DesignSystemProvider>
          <Stack screenOptions={{ headerShown: false }} />
        </DesignSystemProvider>
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}
