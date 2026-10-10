import type { ReactNode } from 'react';
import { View, type StyleProp, type ViewStyle } from 'react-native';
import Svg, { Circle, Rect } from 'react-native-svg';

import { borderWidth, radius, space, Text, useTheme } from '../design-system';

import { infoIcon } from './designSpec';

/** Props of `Notice`. */
export interface NoticeProps {
  /** The notice's sentence, in body text next to the icon. */
  text: string;
  /** Optional footer under the text, such as a link to more notices. */
  children?: ReactNode;
  /** Outer layout only (e.g. margins); the colors and padding belong to the notice. */
  style?: StyleProp<ViewStyle>;
}

/** The design's "i" in a circle, drawn in the info role color. */
function InfoIcon() {
  const { colors } = useTheme();
  const color = colors.role.info.text;
  return (
    <Svg
      width={infoIcon.size}
      height={infoIcon.size}
      viewBox="0 0 20 20"
      style={{ marginTop: infoIcon.offsetTop }}
      accessibilityElementsHidden
      importantForAccessibility="no"
    >
      <Circle cx={10} cy={10} r={8.25} fill="none" stroke={color} strokeWidth={infoIcon.strokeWidth} />
      <Rect x={9.25} y={8.5} width={1.5} height={6} rx={0.75} fill={color} />
      <Circle cx={10} cy={6.2} r={1} fill={color} />
    </Svg>
  );
}

/**
 * Info callout from the screen designs: the info role background and border, the "i" icon, and a
 * sentence. Only the info tone exists, because it is the only one the designs define.
 */
export function Notice({ text, children, style }: NoticeProps) {
  const { colors } = useTheme();

  return (
    <View
      style={[
        {
          gap: space[3],
          padding: space[4],
          borderRadius: radius.lg,
          borderWidth: borderWidth.hairline,
          borderColor: colors.role.info.border,
          backgroundColor: colors.role.info.bg,
        },
        style,
      ]}
    >
      <View style={{ flexDirection: 'row', alignItems: 'flex-start', gap: space[3] }}>
        <InfoIcon />
        <Text style={{ flex: 1 }}>{text}</Text>
      </View>
      {children}
    </View>
  );
}
