import type { ReactNode } from 'react';
import { KeyboardAvoidingView, Modal, Pressable, ScrollView, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { space, useThemedStyles } from '../../../design-system';
import { useKeyboardVisible } from '../../hooks/useKeyboardVisible';

import { createBottomSheetStyles } from './BottomSheet.styles';

/** Props of `BottomSheet`. */
export interface BottomSheetProps {
  /** Accessible name of the sheet, usually its title ("Nuevo gasto", "Pagar Renta"). */
  label: string;
  /** Called by a tap on the backdrop and by Android's Back. The caller decides whether to close. */
  onClose: () => void;
  /** The sheet's content under the handle, spaced by 20px; it scrolls when it does not fit. */
  children: ReactNode;
  /** Drawn inside the sheet's modal, over the sheet (e.g. a dialog or a date picker it opens). */
  overlay?: ReactNode;
}

/**
 * Bottom sheet from the screen designs, over the scrim: a floating surface with a grab handle whose
 * content scrolls and stays above the keyboard. Mount it only while open.
 */
export function BottomSheet({ label, onClose, children, overlay }: BottomSheetProps) {
  const styles = useThemedStyles(createBottomSheetStyles);
  const insets = useSafeAreaInsets();
  const keyboardVisible = useKeyboardVisible();

  return (
    <Modal
      visible
      transparent
      animationType="fade"
      onRequestClose={onClose}
      // Android: draw under the status and navigation bars, so the backdrop covers the whole screen.
      statusBarTranslucent
      navigationBarTranslucent
    >
      <KeyboardAvoidingView behavior="padding" style={styles.container}>
        <Pressable accessibilityRole="button" accessibilityLabel="Cerrar" onPress={onClose} style={styles.backdrop} />
        <View accessibilityViewIsModal aria-label={label} style={styles.sheet}>
          <ScrollView
            keyboardShouldPersistTaps="handled"
            contentContainerStyle={[
              styles.content,
              // Above the keyboard the bottom safe area is covered, so its inset would only add a gap.
              { paddingBottom: space[8] + (keyboardVisible ? 0 : insets.bottom) },
            ]}
          >
            <View accessibilityElementsHidden importantForAccessibility="no" style={styles.handle} />
            {children}
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
      {overlay}
    </Modal>
  );
}
