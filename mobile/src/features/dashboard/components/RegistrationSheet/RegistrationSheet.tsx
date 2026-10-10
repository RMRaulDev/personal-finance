import { useState } from 'react';
import { KeyboardAvoidingView, Modal, Pressable, ScrollView, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { AccountResponse } from '../../../../api';
import {
  AmountField,
  Button,
  ChoiceGroup,
  space,
  Text,
  useThemedStyles,
  type ChoiceOption,
} from '../../../../design-system';
import { formatAmountInput, parseAmountInput } from '../../../../lib';
import { DetailRow, useKeyboardVisible } from '../../../../shared';

import {
  accountOptions,
  AMOUNT_REQUIRED_ERROR,
  registrationTitle,
  type RegistrationType,
} from './RegistrationSheet.model';
import { createRegistrationSheetStyles } from './RegistrationSheet.styles';

/** What the sheet hands over on "Guardar", already validated. */
export interface RegistrationInput {
  type: RegistrationType;
  amountCents: number;
  /** Null when the user chose no category: the category is optional. */
  categoryId: string | null;
  /** Undefined only when no active account is loaded; the screen explains it. */
  accountId: string | undefined;
}

/** Props of `RegistrationSheet`. */
export interface RegistrationSheetProps {
  type: RegistrationType;
  /** Category chips for this type (`categoryOptions`). */
  categories: ChoiceOption<string>[];
  /** Active accounts: one is shown as a fact, several as a choice. */
  accounts: AccountResponse[];
  /** Account preselected when there are several (`defaultAccountId`). */
  defaultAccountId: string | undefined;
  /** True while the registration runs: "Guardar" and "Cancelar" are inactive and the sheet stays open. */
  saving: boolean;
  /**
   * Why the last "Guardar" failed (a server error, or no account to register in), shown under the
   * amount until the amount is edited.
   */
  serverError: string | undefined;
  /** Called when the amount is edited, so the screen clears `serverError`. */
  onDismissError: () => void;
  /** Registers the operation. The screen closes the sheet when it succeeds. */
  onSave: (input: RegistrationInput) => void;
  /** Closes the sheet without saving. */
  onClose: () => void;
}

/**
 * Bottom sheet to register an expense or an income: the amount (focused on open), an optional
 * category, the account, and "Cancelar" / "Guardar". Tapping the backdrop or Android's Back closes
 * it without saving. Mounted only while open, so every opening starts empty.
 */
export function RegistrationSheet({
  type,
  categories,
  accounts,
  defaultAccountId,
  saving,
  serverError,
  onDismissError,
  onSave,
  onClose,
}: RegistrationSheetProps) {
  const styles = useThemedStyles(createRegistrationSheetStyles);
  const insets = useSafeAreaInsets();
  const keyboardVisible = useKeyboardVisible();
  const [amount, setAmount] = useState('');
  const [amountSubmitted, setAmountSubmitted] = useState(false);
  const [categoryId, setCategoryId] = useState<string | null>(null);
  // Undefined until the user picks one: the default applies, even if the accounts load after opening.
  const [pickedAccountId, setPickedAccountId] = useState<string | undefined>(undefined);
  const accountId = pickedAccountId ?? defaultAccountId;
  const title = registrationTitle(type);

  const parsed = parseAmountInput(amount);
  const amountValid = parsed.kind === 'ok' && parsed.cents > 0;
  // A negative amount is explained as soon as it is typed; empty or $0 only after "Guardar".
  const amountError =
    parsed.kind === 'negative' || (amountSubmitted && !amountValid) ? AMOUNT_REQUIRED_ERROR : serverError;

  /** "Guardar" is always active (unless saving): it says what is missing instead. */
  function save() {
    if (parsed.kind !== 'ok' || parsed.cents <= 0) {
      setAmountSubmitted(true);
      return;
    }
    onSave({ type, amountCents: parsed.cents, categoryId, accountId });
  }

  /** Closes without saving, unless a registration is running: its result must reach the sheet. */
  function close() {
    if (!saving) {
      onClose();
    }
  }

  return (
    <Modal
      visible
      transparent
      animationType="fade"
      onRequestClose={close}
      // Android: draw under the status and navigation bars, so the backdrop covers the whole screen.
      statusBarTranslucent
      navigationBarTranslucent
    >
      <KeyboardAvoidingView behavior="padding" style={styles.container}>
        <Pressable accessibilityRole="button" accessibilityLabel="Cerrar" onPress={close} style={styles.backdrop} />
        <View accessibilityViewIsModal aria-label={title} style={styles.sheet}>
          <ScrollView
            keyboardShouldPersistTaps="handled"
            contentContainerStyle={[
              styles.content,
              // Above the keyboard the bottom safe area is covered, so its inset would only add a gap.
              { paddingBottom: space[8] + (keyboardVisible ? 0 : insets.bottom) },
            ]}
          >
            <View accessibilityElementsHidden importantForAccessibility="no" style={styles.handle} />
            <Text variant="heading" accessibilityRole="header">
              {title}
            </Text>
            <AmountField
              label="Importe"
              value={amount}
              onChangeText={(text) => {
                setAmount(formatAmountInput(text));
                setAmountSubmitted(false);
                onDismissError();
              }}
              autoFocus
              error={amountError}
            />
            {categories.length > 0 && (
              <ChoiceGroup
                label="Categoría (opcional)"
                options={categories}
                value={categoryId}
                onChange={setCategoryId}
                allowDeselect
              />
            )}
            {accounts.length === 1 && accounts[0] && (
              // The only active account is a fact, not a choice: shown under a divider.
              <View style={styles.accountFact}>
                <DetailRow label="Cuenta" value={accounts[0].name} />
              </View>
            )}
            {accounts.length > 1 && (
              <ChoiceGroup
                label="Cuenta"
                options={accountOptions(accounts)}
                value={accountId ?? null}
                // Without `allowDeselect` the value is never null; keep the current one if it were.
                onChange={(value) => setPickedAccountId(value ?? accountId)}
              />
            )}
            <View style={styles.actions}>
              <Button variant="ghost" size="lg" inactive={saving} onPress={close}>
                Cancelar
              </Button>
              <View style={styles.primarySlot}>
                <Button variant="primary" size="lg" fullWidth inactive={saving} onPress={save}>
                  Guardar
                </Button>
              </View>
            </View>
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}
