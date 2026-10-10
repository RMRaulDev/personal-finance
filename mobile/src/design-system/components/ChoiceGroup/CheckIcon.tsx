import Svg, { Path } from 'react-native-svg';

/** Size of the check icon and its stroke, from the design's chip markup (16px, 1.75 stroke). */
const CHECK_ICON = { size: 16, strokeWidth: 1.75 } as const;

/** Props of `CheckIcon`. */
interface CheckIconProps {
  color: string;
}

/** The check drawn before the label of the selected chip, so color never marks it alone. */
export function CheckIcon({ color }: CheckIconProps) {
  return (
    <Svg
      width={CHECK_ICON.size}
      height={CHECK_ICON.size}
      viewBox="0 0 16 16"
      accessibilityElementsHidden
      importantForAccessibility="no"
    >
      <Path
        d="M3.5 8.5l3 3 6-7"
        fill="none"
        stroke={color}
        strokeWidth={CHECK_ICON.strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}
