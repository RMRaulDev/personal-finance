import { Text as RNText, View } from 'react-native';

import { useThemedStyles } from '../../theme';
import type { Role } from '../../tokens';

import { createBadgeStyles, type BadgeTone } from './Badge.styles';

/** `Badge` props: the status text and, when it applies, its color role. */
export interface BadgeProps {
  /** 1–2 words that state the status: color never carries the meaning alone. */
  children: string;
  role?: Role;
}

/** Equivalent to `r-badge`: a short, non-interactive status or category label. */
export function Badge({ children, role }: BadgeProps) {
  const styles = useThemedStyles(createBadgeStyles);
  const tone: BadgeTone = role ?? 'neutral';

  return (
    <View style={[styles.badge, styles[tone]]}>
      <RNText numberOfLines={1} style={[styles.label, styles[`${tone}Label` as const]]}>
        {children}
      </RNText>
    </View>
  );
}
