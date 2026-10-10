import { Text } from '../../../../design-system';

/** Props of `Strong`. */
interface StrongProps {
  children: string;
}

/** Emphasized inline amount inside a secondary sentence. */
export function Strong({ children }: StrongProps) {
  return (
    <Text variant="body-sm" tabular weight={500}>
      {children}
    </Text>
  );
}
