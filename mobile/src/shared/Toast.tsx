import { useEffect } from 'react';
import { AccessibilityInfo, Platform, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { borderWidth, Button, control, radius, space, Text, useTheme } from '../design-system';

import type { ToastMessage } from './useToast';

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

/** Floating message at the bottom of the screen, with an optional action such as "Deshacer". */
export function Toast({ toast, onDismiss, bottom }: ToastProps) {
  const theme = useTheme();
  const { colors } = theme;
  const insets = useSafeAreaInsets();

  useEffect(() => {
    // The toast appears without focus moving to it. Android announces it through the live region
    // below; iOS has no live regions, so it is announced explicitly (doing both reads it twice on
    // Android).
    if (Platform.OS === 'ios') {
      AccessibilityInfo.announceForAccessibility(toast.text);
    }
  }, [toast]);

  return (
    <View
      accessibilityLiveRegion="polite"
      style={{
        position: 'absolute',
        left: space[4],
        right: space[4],
        bottom: bottom ?? space[8] + insets.bottom,
        flexDirection: 'row',
        alignItems: 'center',
        gap: space[3],
        minHeight: control.lg,
        paddingVertical: space[2],
        paddingRight: space[2],
        paddingLeft: space[4],
        borderRadius: radius.lg,
        borderWidth: borderWidth.hairline,
        borderColor: colors.border.default,
        backgroundColor: colors.surface.overlay,
        // It floats over the content, so it is one of the few elements allowed a shadow.
        boxShadow: theme.shadows.md,
      }}
    >
      <Text variant="body-sm" style={{ flex: 1 }}>
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
