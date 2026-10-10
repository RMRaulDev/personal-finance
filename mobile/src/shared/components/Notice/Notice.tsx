import type { ReactNode } from 'react';
import { View, type StyleProp, type ViewStyle } from 'react-native';

import { Text, useThemedStyles } from '../../../design-system';

import { InfoIcon } from './InfoIcon';
import { createNoticeStyles } from './Notice.styles';

/** Props of `Notice`. */
export interface NoticeProps {
  /** The notice's sentence, in body text next to the icon. */
  text: string;
  /** Optional footer under the text, such as a link to more notices. */
  children?: ReactNode;
  /** Outer layout only (e.g. margins); the colors and padding belong to the notice. */
  style?: StyleProp<ViewStyle>;
}

/**
 * Info callout from the screen designs: the info role background and border, the "i" icon, and a
 * sentence. Only the info tone exists, because it is the only one the designs define.
 */
export function Notice({ text, children, style }: NoticeProps) {
  const styles = useThemedStyles(createNoticeStyles);

  return (
    <View style={[styles.notice, style]}>
      <View style={styles.content}>
        <InfoIcon />
        <Text style={styles.text}>{text}</Text>
      </View>
      {children}
    </View>
  );
}
