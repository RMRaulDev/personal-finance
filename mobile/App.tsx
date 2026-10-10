import { StatusBar } from 'expo-status-bar';

import { DesignSystemProvider, Root, Text } from './src/design-system';

/** App root: mounts the design system and the first screen. */
export default function App() {
  return (
    <DesignSystemProvider>
      <Root style={{ alignItems: 'center', justifyContent: 'center' }}>
        <Text variant="title-1">Hola mundo</Text>
        <StatusBar style="auto" />
      </Root>
    </DesignSystemProvider>
  );
}
