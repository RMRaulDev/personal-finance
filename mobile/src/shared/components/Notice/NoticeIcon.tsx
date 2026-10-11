import Svg, { Circle, Path, Rect } from 'react-native-svg';

import { useTheme, useThemedStyles } from '../../../design-system';

import { createNoticeIconStyles, NOTICE_ICON } from './NoticeIcon.styles';
import type { NoticeTone } from './Notice.styles';

/** Props of `NoticeIcon`. */
interface NoticeIconProps {
  tone: NoticeTone;
}

/**
 * The design's icon of each notice tone, in that role's text color: an "i" for info, a clock for
 * warning (something is overdue), and an "!" for danger (something failed). With the notice's words,
 * so color never carries the tone alone.
 */
export function NoticeIcon({ tone }: NoticeIconProps) {
  const { colors } = useTheme();
  const styles = useThemedStyles(createNoticeIconStyles);
  const color = colors.role[tone].text;
  const stroke = { fill: 'none', stroke: color, strokeWidth: NOTICE_ICON.strokeWidth } as const;

  return (
    <Svg
      width={NOTICE_ICON.size}
      height={NOTICE_ICON.size}
      viewBox="0 0 20 20"
      style={styles.icon}
      accessibilityElementsHidden
      importantForAccessibility="no"
    >
      <Circle cx={10} cy={10} r={8.25} {...stroke} />
      {tone === 'info' && (
        <>
          <Rect x={9.25} y={8.5} width={1.5} height={6} rx={0.75} fill={color} />
          <Circle cx={10} cy={6.2} r={1} fill={color} />
        </>
      )}
      {tone === 'warning' && <Path d="M10 5.5V10l3 2" {...stroke} strokeLinecap="round" strokeLinejoin="round" />}
      {tone === 'danger' && (
        <>
          <Path d="M10 5.75v5" {...stroke} strokeLinecap="round" />
          <Circle cx={10} cy={13.75} r={1} fill={color} />
        </>
      )}
    </Svg>
  );
}
