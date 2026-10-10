import { Inter_400Regular } from '@expo-google-fonts/inter/400Regular';
import { Inter_500Medium } from '@expo-google-fonts/inter/500Medium';
import { Inter_600SemiBold } from '@expo-google-fonts/inter/600SemiBold';
import { JetBrainsMono_400Regular } from '@expo-google-fonts/jetbrains-mono/400Regular';
import { useFonts } from 'expo-font';
import type { ReactNode } from 'react';

import { ThemeProvider } from './theme';
import type { ColorScheme } from './tokens';

interface DesignSystemProviderProps {
  /** Fuerza un tema. Sin valor sigue el tema del sistema. */
  scheme?: ColorScheme;
  children: ReactNode;
}

/** Carga las fuentes del sistema y provee el tema. Envuelve la app una sola vez. */
export function DesignSystemProvider({ scheme, children }: DesignSystemProviderProps) {
  const [loaded, error] = useFonts({
    Inter_400Regular,
    Inter_500Medium,
    Inter_600SemiBold,
    JetBrainsMono_400Regular,
  });

  // Si las fuentes fallan, se sigue con la fuente del sistema en lugar de bloquear la app.
  if (!loaded && !error) {
    return null;
  }

  return <ThemeProvider scheme={scheme}>{children}</ThemeProvider>;
}
