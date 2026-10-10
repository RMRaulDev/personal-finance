import { useEffect } from 'react';
import { AccessibilityInfo, Platform, View } from 'react-native';

import { ActivityBar, space, Text } from '../../design-system';

import { preparationBottomPadding } from './designSpec';

/** Props of `PreparationView`. */
export interface PreparationViewProps {
  /** The trimmed account name, for "Preparando {nombre}…". */
  accountName: string;
}

/** The wait while the server creates the account and its categories. It offers no actions. */
export function PreparationView({ accountName }: PreparationViewProps) {
  const title = `Preparando ${accountName}…`;

  useEffect(() => {
    // Android reads the live region below; iOS has no live regions, so it is announced explicitly.
    if (Platform.OS === 'ios') {
      AccessibilityInfo.announceForAccessibility(title);
    }
  }, [title]);

  return (
    <View
      style={{
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        gap: space[5],
        paddingHorizontal: space[8],
        paddingBottom: preparationBottomPadding,
      }}
    >
      <ActivityBar accessibilityLabel={title} />
      <View accessibilityLiveRegion="polite" style={{ gap: space[2] }}>
        <Text variant="heading" accessibilityRole="header" style={{ textAlign: 'center' }}>
          {title}
        </Text>
        <Text color="secondary" style={{ textAlign: 'center' }}>
          Creamos tu cuenta y tus categorías.
        </Text>
      </View>
    </View>
  );
}
