import { useEffect, useState } from 'react';
import { Keyboard, Platform } from 'react-native';

/**
 * Whether the software keyboard is on screen, kept up to date. Lets a screen drop the bottom safe
 * area padding under content that sits right above the keyboard, where the inset no longer applies.
 */
export function useKeyboardVisible(): boolean {
  const [visible, setVisible] = useState(() => Keyboard.isVisible());

  useEffect(() => {
    // iOS sends "will" events, in step with the keyboard animation; Android only sends "did" events.
    const showEvent = Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow';
    const hideEvent = Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide';
    const show = Keyboard.addListener(showEvent, () => setVisible(true));
    const hide = Keyboard.addListener(hideEvent, () => setVisible(false));
    return () => {
      show.remove();
      hide.remove();
    };
  }, []);

  return visible;
}
