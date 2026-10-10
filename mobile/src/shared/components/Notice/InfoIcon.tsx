import Svg, { Circle, Rect } from 'react-native-svg';

import { useTheme, useThemedStyles } from '../../../design-system';

import { createInfoIconStyles, INFO_ICON } from './InfoIcon.styles';

/** The design's "i" in a circle, drawn in the info role color. */
export function InfoIcon() {
  const { colors } = useTheme();
  const styles = useThemedStyles(createInfoIconStyles);
  const color = colors.role.info.text;
  return (
    <Svg
      width={INFO_ICON.size}
      height={INFO_ICON.size}
      viewBox="0 0 20 20"
      style={styles.icon}
      accessibilityElementsHidden
      importantForAccessibility="no"
    >
      <Circle cx={10} cy={10} r={8.25} fill="none" stroke={color} strokeWidth={INFO_ICON.strokeWidth} />
      <Rect x={9.25} y={8.5} width={1.5} height={6} rx={0.75} fill={color} />
      <Circle cx={10} cy={6.2} r={1} fill={color} />
    </Svg>
  );
}
