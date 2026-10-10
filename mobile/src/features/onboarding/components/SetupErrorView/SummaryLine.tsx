import { View } from 'react-native';

import { useThemedStyles } from '../../../../design-system';
import { DetailRow } from '../../../../shared';

import { createSummaryLineStyles } from './SummaryLine.styles';

/** Props of `SummaryLine`. */
interface SummaryLineProps {
  label: string;
  value: string;
  /** Fixed-width digits, for the amount. */
  tabular?: boolean;
}

/** One padded row of the summary: what was sent, as a shared `DetailRow`. */
export function SummaryLine({ label, value, tabular }: SummaryLineProps) {
  const styles = useThemedStyles(createSummaryLineStyles);

  return (
    <View style={styles.line}>
      <DetailRow label={label} value={value} tabular={tabular} />
    </View>
  );
}
