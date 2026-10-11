import { useContext } from 'react';

import { ToastContext, type ToastState } from './ToastProvider';

/**
 * The app's toast (`ToastProvider`): the one on display, `show(text, action?)`, and `hide()`.
 * Throws if used outside `ToastProvider`.
 */
export function useToast(): ToastState {
  const state = useContext(ToastContext);
  if (!state) {
    throw new Error('useToast must be used inside <ToastProvider>');
  }
  return state;
}
