import { useState } from 'react';
import { Pressable } from 'react-native';

import { control, focusRing, radius, space, Text, useTheme } from '../../../../design-system';
import { Notice } from '../../../../shared';

/** Props of `AccountNotice`. */
export interface AccountNoticeProps {
  /** Notice for the first account that falls short. */
  text: string;
  /** How many other accounts also fall short; 0 hides the "y N avisos más" link. */
  extraCount: number;
  /** Shows the other notices. */
  onShowExtra: () => void;
}

/**
 * Notice for an account that may not cover its own commitments even when the total does. The
 * design's "Transferir $X" button is left out: the API does not say which account to take the
 * money from (docs/backlog.md §9.4).
 */
export function AccountNotice({ text, extraCount, onShowExtra }: AccountNoticeProps) {
  const theme = useTheme();
  const [linkFocused, setLinkFocused] = useState(false);
  const extraLabel = extraCount === 1 ? 'y 1 aviso más' : `y ${extraCount} avisos más`;

  return (
    // Pulled up toward the available amount, as in the design, because it qualifies it.
    <Notice text={text} style={{ marginTop: -space[4] }}>
      {extraCount > 0 && (
        <Pressable
          accessibilityRole="button"
          onPress={onShowExtra}
          onFocus={() => setLinkFocused(true)}
          onBlur={() => setLinkFocused(false)}
          style={[
            {
              alignSelf: 'center',
              justifyContent: 'center',
              // The text is small, but the touch target keeps the 48px mobile control height.
              minHeight: control.lg,
              paddingHorizontal: space[3],
              borderRadius: radius.md,
            },
            linkFocused && focusRing(theme),
          ]}
        >
          {/* 12px / 500 is the design system caption style. */}
          <Text variant="caption" color="secondary" style={{ textDecorationLine: 'underline' }}>
            {extraLabel}
          </Text>
        </Pressable>
      )}
    </Notice>
  );
}
