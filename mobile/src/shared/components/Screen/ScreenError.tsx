import { Button, Stack, Text, useThemedStyles } from '../../../design-system';

import { CenteredState } from './CenteredState';
import { createScreenErrorStyles } from './ScreenError.styles';

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
  const styles = useThemedStyles(createScreenErrorStyles);

  return (
    <CenteredState>
      <Stack style={styles.content}>
        <Text color="secondary" style={styles.message}>
          {message}
        </Text>
        <Button onPress={onRetry}>Reintentar</Button>
      </Stack>
    </CenteredState>
  );
}
