import { StyleSheet } from 'react-native';

import { focusRing, type Theme } from '../../theme';
import { borderWidth, control, fontFamily, radius, space } from '../../tokens';

/** Label sizes of `r-btn` in components.css: 14px for `sm` and `md`, 16px for `lg`. */
const LABEL_SIZE = { sm: 14, md: 14, lg: 16 } as const;

/**
 * Styles of `Button`: the base box, its width modes, one entry per size (`sm`, `md`, `lg`) and
 * label size (`smLabel`, …), one entry per variant at rest and pressed (`primary`,
 * `primaryPressed`, …) with its label color (`primaryLabel`, …), and the inactive and focused states.
 */
export function createButtonStyles(theme: Theme) {
  const { colors } = theme;
  return StyleSheet.create({
    button: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center',
      alignSelf: 'flex-start',
      gap: space[2],
      borderRadius: radius.md,
      borderWidth: borderWidth.hairline,
    },
    fullWidth: { alignSelf: 'stretch' },
    sm: { height: control.sm, paddingHorizontal: space[3] },
    md: { height: control.md, paddingHorizontal: space[4] },
    lg: { height: control.lg, paddingHorizontal: space[6] },
    label: { fontFamily: fontFamily.sans[500] },
    smLabel: { fontSize: LABEL_SIZE.sm },
    mdLabel: { fontSize: LABEL_SIZE.md },
    lgLabel: { fontSize: LABEL_SIZE.lg },
    // While pressed a button takes the web version's hover color.
    primary: { backgroundColor: colors.role.accent.fill, borderColor: 'transparent' },
    primaryPressed: { backgroundColor: colors.role.accent.fillHover },
    primaryLabel: { color: colors.role.accent.on },
    secondary: { backgroundColor: colors.surface.raised, borderColor: colors.border.control },
    secondaryPressed: { backgroundColor: colors.surface.hover },
    secondaryLabel: { color: colors.text.primary },
    ghost: { backgroundColor: 'transparent', borderColor: 'transparent' },
    ghostPressed: { backgroundColor: colors.surface.hover },
    ghostLabel: { color: colors.text.primary },
    inactive: { backgroundColor: colors.surface.hover, borderColor: 'transparent' },
    inactiveLabel: { color: colors.text.disabled },
    focused: focusRing(theme),
  });
}
