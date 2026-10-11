import { View } from 'react-native';
import Svg, { Path, Rect } from 'react-native-svg';

import { Text, useTheme, useThemedStyles } from '../../../../design-system';

import { ARCHIVE_ICON, createArchivedNoteStyles } from './ArchivedNote.styles';

/** Props of `ArchivedNote`. */
export interface ArchivedNoteProps {
  /** What being archived means ("Archivado el 20 ago. Ya no cuenta en Disponible…"). */
  text: string;
}

/** Card at the top of an archived obligation's detail: the archive box icon and what it means. */
export function ArchivedNote({ text }: ArchivedNoteProps) {
  const { colors } = useTheme();
  const styles = useThemedStyles(createArchivedNoteStyles);
  const stroke = {
    fill: 'none',
    stroke: colors.text.secondary,
    strokeWidth: ARCHIVE_ICON.strokeWidth,
  } as const;

  return (
    <View style={styles.note}>
      <Svg
        width={ARCHIVE_ICON.size}
        height={ARCHIVE_ICON.size}
        viewBox="0 0 20 20"
        style={styles.icon}
        accessibilityElementsHidden
        importantForAccessibility="no"
      >
        <Rect x={2.75} y={4} width={14.5} height={4} rx={1} {...stroke} />
        <Path d="M4 8v7.25c0 .55.45 1 1 1h10c.55 0 1-.45 1-1V8M8 11.25h4" {...stroke} strokeLinecap="round" />
      </Svg>
      <Text style={styles.text}>{text}</Text>
    </View>
  );
}
