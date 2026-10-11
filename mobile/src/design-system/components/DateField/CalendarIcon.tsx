import Svg, { Path, Rect } from 'react-native-svg';

import { useTheme } from '../../theme';

/** Size and stroke of the calendar icon, from the date picker proposal: 20px, 1.5px stroke. */
const CALENDAR_ICON = { size: 20, strokeWidth: 1.5 } as const;

/** The calendar drawn at the end of a `DateField`, in secondary text color. */
export function CalendarIcon() {
  const { colors } = useTheme();
  const color = colors.text.secondary;
  return (
    <Svg
      width={CALENDAR_ICON.size}
      height={CALENDAR_ICON.size}
      viewBox="0 0 20 20"
      accessibilityElementsHidden
      importantForAccessibility="no"
    >
      <Rect
        x={3}
        y={4.5}
        width={14}
        height={12.5}
        rx={2}
        fill="none"
        stroke={color}
        strokeWidth={CALENDAR_ICON.strokeWidth}
      />
      <Path
        d="M3 8.5h14M7 2.75v3M13 2.75v3"
        fill="none"
        stroke={color}
        strokeWidth={CALENDAR_ICON.strokeWidth}
        strokeLinecap="round"
      />
    </Svg>
  );
}
