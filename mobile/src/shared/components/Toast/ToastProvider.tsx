import { createContext, useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { AccessibilityInfo, Platform } from 'react-native';

/** How long a toast without an action stays on screen, in milliseconds. */
const TOAST_DURATION_MS = 4000;

/** How long a toast with an action ("Deshacer") stays, in milliseconds ("Compromisos v1" design: 5 s). */
const TOAST_WITH_ACTION_DURATION_MS = 5000;

/**
 * Longer toast duration while a screen reader is on, so its user has time to reach "Deshacer".
 * Not part of the design, which only covers sighted use.
 */
const TOAST_DURATION_SCREEN_READER_MS = 10000;

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

/** The app's toast: the one on display, and how to show or hide it. */
export interface ToastState {
  /** The toast on display, or null. */
  toast: ToastMessage | null;
  /** Shows a toast, replacing the current one. */
  show: (text: string, action?: ToastAction) => void;
  /** Hides the toast on display. */
  hide: () => void;
}

/** Context that carries the app's toast; read it with `useToast()`, never directly. */
export const ToastContext = createContext<ToastState | null>(null);

/** Props of `ToastProvider`. */
export interface ToastProviderProps {
  children: ReactNode;
}

/**
 * Holds the app's single toast, above the navigation, so a toast shown right before a screen closes
 * (e.g. "Agregaste Renta.") stays on display on the screen it returns to. Each screen draws it with
 * `ToastOutlet`. A new toast replaces the current one; each hides itself after its duration (5 s
 * with an action, 4 s without, longer with a screen reader). Mount it once, in the root layout.
 */
export function ToastProvider({ children }: ToastProviderProps) {
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

  useEffect(() => {
    if (!toast) {
      return undefined;
    }
    // The toast appears without focus moving to it. Android announces it through the toast's live
    // region; iOS has no live regions, so it is announced here, once per toast, even when the
    // screen that shows it changes.
    if (Platform.OS === 'ios') {
      AccessibilityInfo.announceForAccessibility(toast.text);
    }
  }, [toast]);

  const duration = screenReader
    ? TOAST_DURATION_SCREEN_READER_MS
    : toast?.action
      ? TOAST_WITH_ACTION_DURATION_MS
      : TOAST_DURATION_MS;

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

  const value = useMemo<ToastState>(() => ({ toast, show, hide }), [toast, show, hide]);
  return <ToastContext.Provider value={value}>{children}</ToastContext.Provider>;
}
