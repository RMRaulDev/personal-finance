import { Button, Stack, Text } from '../design-system';

import { CenteredState } from './CenteredState';

/** Props of `ScreenError`. */
export interface ScreenErrorProps {
  /** What went wrong, from `errorMessage`. */
  message: string;
  /** Loads the screen's data again. */
  onRetry: () => void;
}

/**
 * A screen that failed to load and has no data to show: the error copy and "Reintentar". The
 * button keeps the default secondary variant, so the screen's single primary action stays free.
 */
export function ScreenError({ message, onRetry }: ScreenErrorProps) {
  return (
    <CenteredState>
      <Stack style={{ alignItems: 'center' }}>
        <Text color="secondary" style={{ textAlign: 'center' }}>
          {message}
        </Text>
        <Button onPress={onRetry}>Reintentar</Button>
      </Stack>
    </CenteredState>
  );
}
