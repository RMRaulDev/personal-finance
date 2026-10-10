import { Inter_400Regular } from '@expo-google-fonts/inter/400Regular';
import { Inter_500Medium } from '@expo-google-fonts/inter/500Medium';
import { Inter_600SemiBold } from '@expo-google-fonts/inter/600SemiBold';
import { JetBrainsMono_400Regular } from '@expo-google-fonts/jetbrains-mono/400Regular';
import { useFonts } from 'expo-font';
import type { ReactNode } from 'react';

import type { ColorScheme } from '../tokens';

import { ThemeProvider } from './ThemeProvider';

interface DesignSystemProviderProps {
  /** Forces a theme. Without a value it follows the system theme. */
  scheme?: ColorScheme;
  children: ReactNode;
}

/** Loads the design system fonts and provides the theme. Wrap the app once. */
export function DesignSystemProvider({ scheme, children }: DesignSystemProviderProps) {
  const [loaded, error] = useFonts({
    Inter_400Regular,
    Inter_500Medium,
    Inter_600SemiBold,
    JetBrainsMono_400Regular,
  });

  // If the fonts fail to load, fall back to the system font instead of blocking the app.
  if (!loaded && !error) {
    return null;
  }

  return <ThemeProvider scheme={scheme}>{children}</ThemeProvider>;
}
