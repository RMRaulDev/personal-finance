import { View } from 'react-native';

import { useThemedStyles } from '../../../../design-system';
import { DetailRow, ListGroup } from '../../../../shared';

import { createFactListStyles } from './FactList.styles';

/** One label and value of a `FactList`. */
export interface Fact {
  label: string;
  value: string;
}

/** Props of `FactList`. */
export interface FactListProps {
  facts: Fact[];
  /** Values in secondary text, for facts that cannot change right now (the locked form). */
  muted?: boolean;
  /** Fixed-width digits in the values, for amounts and dates. */
  tabular?: boolean;
}

/**
 * Label and value rows in a bordered group, as the design lists an obligation's details ("Monto",
 * "Frecuencia", "Se paga desde", …) and the payment's prefilled data.
 */
export function FactList({ facts, muted = false, tabular = false }: FactListProps) {
  const styles = useThemedStyles(createFactListStyles);

  return (
    <ListGroup>
      {facts.map((fact) => (
        <View key={fact.label} style={styles.row}>
          <DetailRow label={fact.label} value={fact.value} muted={muted} tabular={tabular} />
        </View>
      ))}
    </ListGroup>
  );
}
