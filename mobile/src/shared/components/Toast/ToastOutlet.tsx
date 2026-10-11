import { useIsFocused } from 'expo-router';

import { Toast } from './Toast';
import { useToast } from './useToast';

/** Props of `ToastOutlet`. */
export interface ToastOutletProps {
  /**
   * Distance from the bottom of the area the toast floats in (see `Toast`). Defaults to `space[8]`
   * plus the bottom safe area, for a screen without a footer.
   */
  bottom?: number;
}

/**
 * Where a screen draws the app's toast, usually in `Screen`'s `overlay` slot. Only the focused
 * screen draws it, so a screen left underneath in the stack never shows a second copy.
 */
export function ToastOutlet({ bottom }: ToastOutletProps) {
  const { toast, hide } = useToast();
  const focused = useIsFocused();

  if (!toast || !focused) {
    return null;
  }
  return <Toast toast={toast} onDismiss={hide} bottom={bottom} />;
}
