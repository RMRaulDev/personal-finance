import type { ReactNode } from 'react';
import { ScrollView, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button, space, Text, useThemedStyles } from '../../../../design-system';
import { useKeyboardVisible } from '../../../../shared';

import { createOnboardingStepStyles } from './OnboardingStep.styles';

/** Props of `OnboardingStep`. */
export interface OnboardingStepProps {
  /** Step number shown as "Paso N de 2". */
  step: 1 | 2;
  /** Shows "‹ Atrás" in the header (step 2 only). */
  onBack?: () => void;
  title: string;
  /** Explanation under the title. */
  text: string;
  /** The step's field. */
  children: ReactNode;
  /** The step's primary button, pinned below the scrolling content. */
  action: ReactNode;
  /**
   * Footer padding from the design: `relaxed` (16 above, 40 below) for a step that does not open
   * the keyboard, `keyboard` (12 above and below) for a step whose button sits right above it.
   */
  footer: 'relaxed' | 'keyboard';
}

/**
 * Layout of an onboarding step: the header with "‹ Atrás" and "Paso N de 2", the title, text, and
 * field in a scrolling body, and the step's button pinned at the bottom (above the keyboard).
 */
export function OnboardingStep({ step, onBack, title, text, children, action, footer }: OnboardingStepProps) {
  const styles = useThemedStyles(createOnboardingStepStyles);
  const insets = useSafeAreaInsets();
  const keyboardVisible = useKeyboardVisible();
  const relaxed = footer === 'relaxed';
  // Above the keyboard the bottom safe area is covered, so its inset would only add a gap.
  const bottomInset = keyboardVisible ? 0 : insets.bottom;

  return (
    <>
      <View style={styles.header}>
        <View style={styles.backSlot}>
          {onBack && (
            <Button variant="ghost" onPress={onBack} accessibilityLabel="Atrás">
              ‹ Atrás
            </Button>
          )}
        </View>
        <Text variant="body-sm" color="secondary" style={styles.stepLabel}>
          {`Paso ${step} de 2`}
        </Text>
      </View>
      <ScrollView style={styles.body} keyboardShouldPersistTaps="handled" contentContainerStyle={styles.bodyContent}>
        <View style={styles.intro}>
          <Text variant="title-2" accessibilityRole="header">
            {title}
          </Text>
          <Text color="secondary">{text}</Text>
        </View>
        {children}
      </ScrollView>
      <View
        style={[
          styles.footer,
          relaxed ? styles.footerRelaxed : styles.footerKeyboard,
          { paddingBottom: (relaxed ? space[10] : space[3]) + bottomInset },
        ]}
      >
        {action}
      </View>
    </>
  );
}
