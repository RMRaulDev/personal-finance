import { Text, useThemedStyles, type TextProps } from '../../../design-system';

import { createSectionTitleStyles } from './SectionTitle.styles';

/**
 * Props of `SectionTitle`: the design system `Text` props. The weight and the design's size are
 * set by the title; `style` adds layout (e.g. a margin) on top of them.
 */
export interface SectionTitleProps extends TextProps {
  /**
   * Defaults to `header`, so screen readers can jump between sections. Use `text` for a value shown
   * in title style next to a title (e.g. the commitments total).
   */
  accessibilityRole?: TextProps['accessibilityRole'];
}

/** Section title of a screen ("Próximos compromisos", "Reciente"), as the design sizes it. */
export function SectionTitle({ style, ...rest }: SectionTitleProps) {
  const styles = useThemedStyles(createSectionTitleStyles);

  return <Text accessibilityRole="header" weight={600} style={[styles.title, style]} {...rest} />;
}
