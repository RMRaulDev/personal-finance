import { View } from 'react-native';

import type { AvailableToSpend } from '../../api';
import { borderWidth, control, radius, space, Stack, Text, useTheme } from '../../design-system';
import { formatMoney } from '../../lib';

import { heroAmount, shortfallNoteMaxWidth } from './designSpec';

/** Props of `AvailableSummary`. */
export interface AvailableSummaryProps {
  availableToSpend: AvailableToSpend;
  /** Horizon chip text, e.g. "Próximos 14 días · 20 oct". */
  horizonLabel: string;
}

/** Props of `Strong`. */
interface StrongProps {
  children: string;
}

/** Emphasized inline amount inside a secondary sentence. */
function Strong({ children }: StrongProps) {
  return (
    <Text variant="body-sm" tabular weight={500}>
      {children}
    </Text>
  );
}

/**
 * "Disponible para gastar" block: the hero amount, the shortfall note, the balance minus
 * commitments breakdown, and the horizon chip.
 */
export function AvailableSummary({ availableToSpend, horizonLabel }: AvailableSummaryProps) {
  const { colors } = useTheme();
  const { availableCents, balanceCents, committedCents, shortfallCents } = availableToSpend;

  return (
    <Stack style={{ gap: space[2] }}>
      <Text variant="body-sm" color="secondary" weight={500}>
        Disponible para gastar
      </Text>

      <View style={{ flexDirection: 'row', flexWrap: 'wrap', alignItems: 'baseline', gap: space[3] }}>
        <Text
          tabular
          weight={600}
          style={{
            fontSize: heroAmount.fontSize,
            lineHeight: heroAmount.fontSize * heroAmount.lineHeight,
            letterSpacing: heroAmount.fontSize * heroAmount.tracking,
          }}
        >
          {formatMoney(availableCents)}
        </Text>
        {shortfallCents > 0 && (
          // Next to the result, so "Saldo − Compromisos" still adds up while Disponible stays at $0.
          <Text variant="body-sm" color="secondary" style={{ maxWidth: shortfallNoteMaxWidth }}>
            faltan <Strong>{formatMoney(shortfallCents)}</Strong> para cubrir tus compromisos
          </Text>
        )}
      </View>

      <Text variant="body-sm" color="secondary" tabular>
        Saldo <Strong>{formatMoney(balanceCents)}</Strong> − Compromisos <Strong>{formatMoney(committedCents)}</Strong>
      </Text>

      {/* Not pressable: the API horizon is fixed at 14 days (docs/backlog.md §9.1). */}
      <View
        style={{
          marginTop: space[2],
          alignSelf: 'flex-start',
          justifyContent: 'center',
          height: control.sm,
          paddingHorizontal: space[3],
          borderRadius: radius.full,
          borderWidth: borderWidth.hairline,
          borderColor: colors.border.default,
          backgroundColor: colors.surface.raised,
        }}
      >
        {/* 14px / 500: the design system body-sm size with the UI weight. */}
        <Text variant="body-sm" weight={500}>
          {horizonLabel}
        </Text>
      </View>
    </Stack>
  );
}
