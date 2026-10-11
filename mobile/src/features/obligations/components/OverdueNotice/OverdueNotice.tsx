import { View } from 'react-native';

import { Button, Text, useThemedStyles } from '../../../../design-system';
import { Notice } from '../../../../shared';

import { createOverdueNoticeStyles } from './OverdueNotice.styles';

/** Props of `OverdueNotice`. */
export interface OverdueNoticeProps {
  /** "2 pagos vencidos". */
  title: string;
  /** Dates and amount: "5 sep y 5 oct · $550 cada uno". */
  subtitle: string;
  /** "Pagar la más antigua · 5 sep": the screen's only primary action. */
  payLabel: string;
  /** "Saltar todas las vencidas (2)". */
  skipLabel: string;
  /** True while a payment, skip, or undo runs: both buttons are inactive. */
  busy: boolean;
  /** Opens the payment sheet for the oldest overdue date. */
  onPayOldest: () => void;
  /** Asks to confirm skipping every overdue date. */
  onSkipAll: () => void;
}

/**
 * The single block that gathers what is overdue, with its two ways out: pay the oldest or skip them
 * all. Its words invite to resolve without reproach.
 */
export function OverdueNotice({
  title,
  subtitle,
  payLabel,
  skipLabel,
  busy,
  onPayOldest,
  onSkipAll,
}: OverdueNoticeProps) {
  const styles = useThemedStyles(createOverdueNoticeStyles);

  return (
    <Notice
      tone="warning"
      title={title}
      details={
        <>
          <Text variant="body-sm" tabular>
            {subtitle}
          </Text>
          <Text variant="body-sm" color="secondary">
            Págalos si ya los hiciste o sáltalos si este periodo no aplicaban.
          </Text>
        </>
      }
    >
      <View style={styles.actions}>
        <Button variant="primary" size="lg" fullWidth inactive={busy} onPress={onPayOldest}>
          {payLabel}
        </Button>
        <Button size="lg" fullWidth inactive={busy} onPress={onSkipAll}>
          {skipLabel}
        </Button>
      </View>
    </Notice>
  );
}
