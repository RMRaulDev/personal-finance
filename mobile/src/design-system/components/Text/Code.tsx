import { Text as RNText, type TextProps as RNTextProps } from 'react-native';

import { codeScale, fontFamily, textStyles } from '../../tokens';

/** `Code` props: the React Native props plus the size of the surrounding text. */
export interface CodeProps extends RNTextProps {
  /** Size of the surrounding text; code is 0.875 of it. */
  parentSize?: number;
}

/**
 * Monospaced text for code and data, meant to be nested inside `Text`. Its size depends on
 * `parentSize`, so its one-line style is computed while rendering.
 */
export function Code({ parentSize = textStyles.body.size, style, ...rest }: CodeProps) {
  return <RNText style={[{ fontFamily: fontFamily.mono, fontSize: parentSize * codeScale }, style]} {...rest} />;
}
