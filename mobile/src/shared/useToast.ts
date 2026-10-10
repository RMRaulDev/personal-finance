import { useCallback, useEffect, useRef, useState } from 'react';
import { AccessibilityInfo } from 'react-native';

import { toastDurationMs, toastDurationScreenReaderMs } from './designSpec';

/** Optional button of a toast, e.g. "Deshacer". */
export interface ToastAction {
  label: string;
  onPress: () => void;
}

/** A message shown briefly at the bottom of the screen. */
export interface ToastMessage {
  /** Changes on every toast, so a repeated text still restarts the timer. */
  id: number;
  text: string;
  action?: ToastAction;
}

/**
 * State of a screen's toast. A new toast replaces the current one, and each toast hides itself
 * after the design's duration.
 */
export function useToast() {
  const [toast, setToast] = useState<ToastMessage | null>(null);
  const nextId = useRef(0);
  const [screenReader, setScreenReader] = useState(false);

  useEffect(() => {
    // Track the screen reader so toasts stay long enough to be heard and acted on.
    let active = true;
    void AccessibilityInfo.isScreenReaderEnabled().then((enabled) => {
      if (active) {
        setScreenReader(enabled);
      }
    });
    const subscription = AccessibilityInfo.addEventListener('screenReaderChanged', setScreenReader);
    return () => {
      active = false;
      subscription.remove();
    };
  }, []);

  const show = useCallback((text: string, action?: ToastAction) => {
    nextId.current += 1;
    setToast({ id: nextId.current, text, action });
  }, []);

  const hide = useCallback(() => setToast(null), []);

  const duration = screenReader ? toastDurationScreenReaderMs : toastDurationMs;

  useEffect(() => {
    if (!toast) {
      return undefined;
    }
    const timer = setTimeout(() => {
      // Only hide the toast this timer belongs to; a newer one has its own timer.
      setToast((current) => (current?.id === toast.id ? null : current));
    }, duration);
    return () => clearTimeout(timer);
  }, [toast, duration]);

  return { toast, show, hide };
}
