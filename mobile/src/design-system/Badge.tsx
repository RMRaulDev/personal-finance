import { Text as RNText, View } from 'react-native';

import { useTheme } from './theme/ThemeProvider';
import { borderWidth, fontFamily, radius, space, textStyles, type Role } from './tokens/colors';

/** `Badge` props: the status text and, when it applies, its color role. */
export interface BadgeProps {
  /** 1–2 words that state the status: color never carries the meaning alone. */
  children: string;
  role?: Role;
}

/** Equivalent to `r-badge`: a short, non-interactive status or category label. */
export function Badge({ children, role }: BadgeProps) {
  const { colors } = useTheme();
  const tone = role
    ? { backgroundColor: colors.role[role].bg, borderColor: colors.role[role].border, color: colors.role[role].text }
    : { backgroundColor: colors.surface.hover, borderColor: colors.border.subtle, color: colors.text.primary };

  return (
    <View
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        alignSelf: 'flex-start',
        height: 20,
        paddingHorizontal: space[2],
        borderRadius: radius.sm,
        borderWidth: borderWidth.hairline,
        backgroundColor: tone.backgroundColor,
        borderColor: tone.borderColor,
      }}
    >
      <RNText
        numberOfLines={1}
        style={{ fontFamily: fontFamily.sans[500], fontSize: textStyles.caption.size, color: tone.color }}
      >
        {children}
      </RNText>
    </View>
  );
}
