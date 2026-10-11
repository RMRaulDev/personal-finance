import { Fragment } from 'react';
import { View } from 'react-native';

import { Button, PressableRow, Text, useThemedStyles } from '../../../../design-system';

import { createArchivedSectionStyles } from './ArchivedSection.styles';

/** What an archived row shows (`archivedRows` maps it). */
export interface ArchivedRowData {
  id: string;
  name: string;
  amount: string;
  status: string;
  accessibilityLabel: string;
}

/** Props of `ArchivedSection`. */
export interface ArchivedSectionProps {
  rows: ArchivedRowData[];
  /** Text of the toggle ("Archivados (2) ▾"). */
  toggleLabel: string;
  expanded: boolean;
  onToggle: () => void;
  /** Opens an archived obligation's read-only detail. */
  onOpen: (obligationId: string) => void;
}

/**
 * Archived obligations at the end of the list, collapsed behind a counter and expanded in place. The
 * rows are in secondary text and open a read-only detail.
 */
export function ArchivedSection({ rows, toggleLabel, expanded, onToggle, onOpen }: ArchivedSectionProps) {
  const styles = useThemedStyles(createArchivedSectionStyles);

  return (
    <View style={styles.section}>
      <View style={styles.toggle}>
        <Button variant="ghost" size="sm" aria-expanded={expanded} onPress={onToggle}>
          {toggleLabel}
        </Button>
      </View>
      {expanded && (
        <View style={styles.group}>
          {rows.map((row, index) => (
            <Fragment key={row.id}>
              {index > 0 && <View style={styles.divider} />}
              <PressableRow
                accessibilityLabel={row.accessibilityLabel}
                onPress={() => onOpen(row.id)}
                trailing={
                  <Text tabular color="secondary">
                    {row.amount}
                  </Text>
                }
              >
                <View style={styles.texts}>
                  <Text color="secondary" numberOfLines={1}>
                    {row.name}
                  </Text>
                  <Text variant="body-sm" color="secondary">
                    {row.status}
                  </Text>
                </View>
              </PressableRow>
            </Fragment>
          ))}
        </View>
      )}
    </View>
  );
}
