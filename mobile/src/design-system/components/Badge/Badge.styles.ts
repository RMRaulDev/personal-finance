import { StyleSheet } from 'react-native';

import type { Theme } from '../../theme';
import { borderWidth, fontFamily, radius, space, textStyles, type Role } from '../../tokens';

/** Badge height from `r-badge` in components.css; it sits between the spacing steps. */
const BADGE_HEIGHT = 20;

/** Color tone of a badge: a role, or `neutral` when it has none. */
export type BadgeTone = Role | 'neutral';

/**
 * Styles of `Badge`: the container and label, plus one container entry (`accent`, …, `neutral`)
 * and one label entry (`accentLabel`, …) per tone.
 */
export function createBadgeStyles(theme: Theme) {
  const { colors } = theme;
  /** Background and border of a role's tone. */
  const container = (role: Role) => ({
    backgroundColor: colors.role[role].bg,
    borderColor: colors.role[role].border,
  });
  /** Text color of a role's tone. */
  const label = (role: Role) => ({ color: colors.role[role].text });

  return StyleSheet.create({
    badge: {
      flexDirection: 'row',
      alignItems: 'center',
      alignSelf: 'flex-start',
      height: BADGE_HEIGHT,
      paddingHorizontal: space[2],
      borderRadius: radius.sm,
      borderWidth: borderWidth.hairline,
    },
    label: { fontFamily: fontFamily.sans[500], fontSize: textStyles.caption.size },
    neutral: { backgroundColor: colors.surface.hover, borderColor: colors.border.subtle },
    neutralLabel: { color: colors.text.primary },
    accent: container('accent'),
    accentLabel: label('accent'),
    success: container('success'),
    successLabel: label('success'),
    warning: container('warning'),
    warningLabel: label('warning'),
    danger: container('danger'),
    dangerLabel: label('danger'),
    info: container('info'),
    infoLabel: label('info'),
  });
}
