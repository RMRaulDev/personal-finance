import Svg, { Path } from 'react-native-svg';

import { useTheme } from '../../theme';

import { CHEVRON } from './PressableRow.styles';

/** The "›" chevron that says the row opens another screen, in secondary text color. */
export function Chevron() {
  const { colors } = useTheme();
  return (
    <Svg
      width={CHEVRON.size}
      height={CHEVRON.size}
      viewBox="0 0 16 16"
      accessibilityElementsHidden
      importantForAccessibility="no"
    >
      <Path
        d="M6 3.5l4.5 4.5L6 12.5"
        fill="none"
        stroke={colors.text.secondary}
        strokeWidth={CHEVRON.strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}
