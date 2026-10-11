import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button, space, Text, useThemedStyles } from '../../../design-system';

import { createToastStyles } from './Toast.styles';
import type { ToastMessage } from './ToastProvider';

/** Props of `Toast`. */
export interface ToastProps {
  toast: ToastMessage;
  /** Called after the action runs, so the toast closes right away. */
  onDismiss: () => void;
  /**
   * Distance from the bottom of the area the toast floats in. Defaults to `space[8]` plus the
   * bottom safe area, for a screen without a footer. Above a footer (which already clears the safe
   * area), pass the gap the design leaves between the footer and the toast.
   */
  bottom?: number;
}

/**
 * Floating message at the bottom of the screen, with an optional action such as "Deshacer". Android
 * announces it through its live region; `ToastProvider` announces it on iOS.
 */
export function Toast({ toast, onDismiss, bottom }: ToastProps) {
  const styles = useThemedStyles(createToastStyles);
  const insets = useSafeAreaInsets();

  return (
    <View accessibilityLiveRegion="polite" style={[styles.toast, { bottom: bottom ?? space[8] + insets.bottom }]}>
      <Text variant="body-sm" style={styles.text}>
        {toast.text}
      </Text>
      {toast.action && (
        <Button
          variant="ghost"
          size="sm"
          onPress={() => {
            toast.action?.onPress();
            onDismiss();
          }}
        >
          {toast.action.label}
        </Button>
      )}
    </View>
  );
}
