import { useEffect } from 'react';
import { AccessibilityInfo, Platform, View } from 'react-native';

import { ActivityBar, Text, useThemedStyles } from '../../../../design-system';

import { createPreparationViewStyles } from './PreparationView.styles';

/** Props of `PreparationView`. */
export interface PreparationViewProps {
  /** The trimmed account name, for "Preparando {nombre}…". */
  accountName: string;
}

/** The wait while the server creates the account and its categories. It offers no actions. */
export function PreparationView({ accountName }: PreparationViewProps) {
  const styles = useThemedStyles(createPreparationViewStyles);
  const title = `Preparando ${accountName}…`;

  useEffect(() => {
    // Android reads the live region below; iOS has no live regions, so it is announced explicitly.
    if (Platform.OS === 'ios') {
      AccessibilityInfo.announceForAccessibility(title);
    }
  }, [title]);

  return (
    <View style={styles.view}>
      <ActivityBar accessibilityLabel={title} />
      <View accessibilityLiveRegion="polite" style={styles.texts}>
        <Text variant="heading" accessibilityRole="header" style={styles.centered}>
          {title}
        </Text>
        <Text color="secondary" style={styles.centered}>
          Creamos tu cuenta y tus categorías.
        </Text>
      </View>
    </View>
  );
}
