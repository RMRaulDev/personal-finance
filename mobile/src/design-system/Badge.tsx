import { Text as RNText, View } from 'react-native';

import { useTheme } from './theme';
import { borderWidth, fontFamily, radius, space, textStyles, type Role } from './tokens';

export interface BadgeProps {
  /** 1–2 palabras que dicen el estado: el color nunca carga el significado solo. */
  children: string;
  role?: Role;
}

/** Equivale a `r-badge`: etiqueta de estado o categoría, corta y no interactiva. */
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
