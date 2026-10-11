import { View } from 'react-native';

import { Button, Text, useThemedStyles } from '../../../../design-system';
import { ListGroup, SectionTitle } from '../../../../shared';

import { ResolvedIcon } from './ResolvedIcon';
import { createResolvedListStyles } from './ResolvedList.styles';

/** How a resolved occurrence is reverted: cancel the expense that paid it, or reopen a skipped one. */
export type ResolvedAction = { kind: 'undoPayment'; operationId: string } | { kind: 'reopen'; dueDate: string };

/** One resolved occurrence. */
export interface ResolvedRowData {
  key: string;
  /** "Lunes 5 oct". */
  date: string;
  paid: boolean;
  /** "Pagado el 5 oct · $550", "Pagado por adelantado el 10 oct · $1,200", or "Saltado · sin gasto". */
  detail: string;
  /** "Deshacer pago" or "Reabrir". */
  actionLabel: string;
  action: ResolvedAction;
}

/** Props of `ResolvedList`. */
export interface ResolvedListProps {
  rows: ResolvedRowData[];
  /** False for an archived obligation: its history is read-only. */
  canAct: boolean;
  /** True while a payment, skip, or undo runs: every action is inactive. */
  busy: boolean;
  onAction: (row: ResolvedRowData) => void;
}

/** "Resueltos": the latest resolutions, each with the action that reverts it. */
export function ResolvedList({ rows, canAct, busy, onAction }: ResolvedListProps) {
  const styles = useThemedStyles(createResolvedListStyles);

  return (
    <View style={styles.section}>
      <SectionTitle>Resueltos</SectionTitle>
      <ListGroup>
        {rows.map((row) => (
          <View key={row.key} style={styles.item}>
            <ResolvedIcon paid={row.paid} />
            <View style={styles.texts}>
              <Text>{row.date}</Text>
              <Text variant="body-sm" color="secondary" tabular>
                {row.detail}
              </Text>
            </View>
            {canAct && (
              <Button
                variant="ghost"
                size="sm"
                inactive={busy}
                accessibilityLabel={`${row.actionLabel}, ${row.date}`}
                onPress={() => onAction(row)}
              >
                {row.actionLabel}
              </Button>
            )}
          </View>
        ))}
      </ListGroup>
    </View>
  );
}
