import type { ReactNode } from 'react';
import { View, type StyleProp, type ViewStyle } from 'react-native';

import { Text, useThemedStyles } from '../../../design-system';

import { createNoticeStyles, type NoticeTone } from './Notice.styles';
import { NoticeIcon } from './NoticeIcon';

/** Props of `Notice`. */
export interface NoticeProps {
  /**
   * `info` (default) explains, `warning` asks to resolve something overdue, and `danger` says that
   * something failed (it is announced as an alert).
   */
  tone?: NoticeTone;
  /** Optional title above the text, in body weight 600 (e.g. "Pago bloqueado"). */
  title?: string;
  /** The notice's sentence, in body text next to the icon. */
  text?: string;
  /** Extra lines under the title and text, for a notice whose lines have their own styles. */
  details?: ReactNode;
  /** Optional footer under the text, such as a link or the notice's buttons. */
  children?: ReactNode;
  /** Outer layout only (e.g. margins); the colors and padding belong to the notice. */
  style?: StyleProp<ViewStyle>;
}

/**
 * Callout from the screen designs: the tone's background and border, its icon, an optional title,
 * a sentence, and optional actions below.
 */
export function Notice({ tone = 'info', title, text, details, children, style }: NoticeProps) {
  const styles = useThemedStyles(createNoticeStyles);

  return (
    <View accessibilityRole={tone === 'danger' ? 'alert' : undefined} style={[styles.notice, styles[tone], style]}>
      <View style={styles.content}>
        <NoticeIcon tone={tone} />
        <View style={styles.texts}>
          {title !== undefined && <Text weight={600}>{title}</Text>}
          {text !== undefined && <Text>{text}</Text>}
          {details}
        </View>
      </View>
      {children}
    </View>
  );
}
