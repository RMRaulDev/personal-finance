import { View } from 'react-native';
import Svg, { Path } from 'react-native-svg';

import { useTheme, useThemedStyles } from '../../../../design-system';

import { createResolvedIconStyles, RESOLVED_ICON } from './ResolvedIcon.styles';

/** Props of `ResolvedIcon`. */
interface ResolvedIconProps {
  paid: boolean;
}

/**
 * How an occurrence was resolved: a check in the success color when paid, a "skip" arrow in
 * secondary text when skipped. The row's words say it too, so color is never the only cue.
 */
export function ResolvedIcon({ paid }: ResolvedIconProps) {
  const { colors } = useTheme();
  const styles = useThemedStyles(createResolvedIconStyles);
  const color = paid ? colors.role.success.text : colors.text.secondary;

  return (
    <View style={styles.circle} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
      <Svg width={RESOLVED_ICON.glyph} height={RESOLVED_ICON.glyph} viewBox="0 0 16 16">
        {paid ? (
          <Path
            d="M3.5 8.5l3 3 6-7"
            fill="none"
            stroke={color}
            strokeWidth={RESOLVED_ICON.checkStroke}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        ) : (
          <Path
            d="M3 8h7.5M8 4.5L11.5 8 8 11.5M13 4v8"
            fill="none"
            stroke={color}
            strokeWidth={RESOLVED_ICON.skipStroke}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}
      </Svg>
    </View>
  );
}
