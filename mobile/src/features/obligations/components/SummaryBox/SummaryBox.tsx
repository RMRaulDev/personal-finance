import { View } from 'react-native';

import { Text, useThemedStyles } from '../../../../design-system';

import { createSummaryBoxStyles } from './SummaryBox.styles';

/** Props of `SummaryBox`. */
export interface SummaryBoxProps {
  /** The summary sentence ("Pagarás $4,800 cada mes, el día 15, desde el 15 oct."). */
  text: string;
}

/**
 * "Resumen" of the obligation form: what will be saved, in plain language. It is a live region, so a
 * screen reader hears it change as the fields change.
 */
export function SummaryBox({ text }: SummaryBoxProps) {
  const styles = useThemedStyles(createSummaryBoxStyles);

  return (
    <View accessibilityLiveRegion="polite" style={styles.box}>
      <Text variant="body-sm" color="secondary">
        Resumen
      </Text>
      <Text tabular>{text}</Text>
    </View>
  );
}
