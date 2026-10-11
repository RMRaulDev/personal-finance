import { View } from 'react-native';

import type { IsoDate } from '../../../../api';
import { useThemedStyles } from '../../../../design-system';
import { ListGroup, SectionTitle } from '../../../../shared';

import { UpcomingItem } from './UpcomingItem';
import { createUpcomingListStyles } from './UpcomingList.styles';

/** One occurrence of "Por resolver" or "Próximos". */
export interface UpcomingRowData {
  dueDate: IsoDate;
  /** "Jueves 15 oct". */
  date: string;
  amount: string;
  /** Overdue: shown with a "Vencida" badge. */
  overdue: boolean;
  /** "hace 3 días", or "Pendiente · en 5 días". */
  when: string;
}

/** Props of `UpcomingList`. */
export interface UpcomingListProps {
  /** "Por resolver" when something is overdue, "Próximos" otherwise. */
  title: string;
  rows: UpcomingRowData[];
  /** True while a payment, skip, or undo runs: every action is inactive. */
  busy: boolean;
  /** Opens the payment sheet for a date. */
  onPay: (dueDate: IsoDate) => void;
  /** Skips a date right away (with a toast to undo it). */
  onSkip: (dueDate: IsoDate) => void;
}

/** The overdue occurrences and the next pending ones, each with "Pagar" and "Saltar". */
export function UpcomingList({ title, rows, busy, onPay, onSkip }: UpcomingListProps) {
  const styles = useThemedStyles(createUpcomingListStyles);

  return (
    <View style={styles.section}>
      <SectionTitle>{title}</SectionTitle>
      <ListGroup>
        {rows.map((row) => (
          <UpcomingItem
            key={row.dueDate}
            row={row}
            busy={busy}
            onPay={() => onPay(row.dueDate)}
            onSkip={() => onSkip(row.dueDate)}
          />
        ))}
      </ListGroup>
    </View>
  );
}
