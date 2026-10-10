import { View } from 'react-native';

import type { IsoDate, RecentActivityItem } from '../../../../api';
import { Text, useThemedStyles } from '../../../../design-system';
import { operationDisplay, OperationRow, SectionTitle } from '../../../../shared';

import { createRecentListStyles } from './RecentList.styles';

/** Props of `RecentList`. */
export interface RecentListProps {
  /** Operations to show, already limited to what the design shows (`recentOperations`). */
  operations: RecentActivityItem[];
  /** The server's today (`horizon.from`), for "Hoy" and "Ayer". */
  today: IsoDate;
}

/** "Reciente": the latest operations, or the design's empty text. */
export function RecentList({ operations, today }: RecentListProps) {
  const styles = useThemedStyles(createRecentListStyles);

  return (
    <View style={styles.list}>
      <SectionTitle style={styles.title}>Reciente</SectionTitle>
      {operations.length > 0 ? (
        operations.map((operation) => (
          <OperationRow key={operation.id} operation={operationDisplay(operation, today)} />
        ))
      ) : (
        <Text variant="body-sm" color="secondary">
          Lo que registres aparecerá aquí.
        </Text>
      )}
    </View>
  );
}
