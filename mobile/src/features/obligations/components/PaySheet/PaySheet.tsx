import { useState, type ReactNode } from 'react';
import { View } from 'react-native';

import type { AccountResponse, IsoDate } from '../../../../api';
import {
  AmountField,
  Button,
  ChoiceGroup,
  DatePickerSheet,
  Text,
  useThemedStyles,
  type ChoiceOption,
} from '../../../../design-system';
import { addDays, amountInputFromCents, formatAmountInput, formatMoney, parseAmountInput } from '../../../../lib';
import { accountOptions, BottomSheet, Notice } from '../../../../shared';
import { FactList } from '../FactList';

import {
  changedAmountHelp,
  dateChoice,
  dateOptions,
  dueText,
  OTHER_DATE,
  PAY_ACCOUNT_ERROR,
  PAY_AMOUNT_ERROR,
  PAY_CATEGORY_ERROR,
  PAY_DATE_HELP,
  PAY_DATE_MAX_DAYS_BACK,
  paymentFacts,
  type PayError,
} from './PaySheet.model';
import { createPaySheetStyles } from './PaySheet.styles';

/** What the sheet hands over on "Pagar", already validated. */
export interface PaymentInput {
  amountCents: number;
  accountId: string;
  categoryId: string;
  operationDate: IsoDate;
}

/** Props of `PaySheet`. */
export interface PaySheetProps {
  /** Obligation name, for "Pagar {nombre}". */
  name: string;
  /** The obligation's amount: prefilled, and the base the "only this payment" help compares with. */
  amountCents: number;
  dueDate: IsoDate;
  /** The server's today: the default payment date and the latest one allowed. */
  today: IsoDate;
  /** Active accounts. */
  accounts: AccountResponse[];
  /** The obligation's account when it is active; null makes the user choose (the sheet opens expanded). */
  defaultAccountId: string | null;
  /** Active expense categories (`categoryOptions`). */
  categories: ChoiceOption<string>[];
  /** The obligation's category when it is active; null makes the user choose (the sheet opens expanded). */
  defaultCategoryId: string | null;
  /** True while the payment runs: the actions are inactive and the sheet stays open. */
  saving: boolean;
  /** Why the last "Pagar" failed, shown in the sheet until something changes. */
  error: PayError | undefined;
  /** Called when the amount or the account changes, so the screen clears `error`. */
  onDismissError: () => void;
  /** Pays with what the sheet shows. The screen closes the sheet when it succeeds. */
  onConfirm: (payment: PaymentInput) => void;
  /** Closes the sheet without paying. */
  onClose: () => void;
  /** Drawn over the sheet, inside its modal (e.g. the "already resolved" dialog). */
  overlay?: ReactNode;
}

/**
 * "Pagar {nombre}": the common case is to confirm what is prefilled (amount, account, category, and
 * today's date) in a compact list; "Cambiar…" shows the fields. A changed amount only affects this
 * payment, and the date can be today, yesterday, the due date, or another past date, never a future
 * one. Failures (e.g. "Saldo insuficiente") show in the sheet, which stays open. Mounted only while
 * open, so every opening starts from the obligation's defaults.
 */
export function PaySheet({
  name,
  amountCents,
  dueDate,
  today,
  accounts,
  defaultAccountId,
  categories,
  defaultCategoryId,
  saving,
  error,
  onDismissError,
  onConfirm,
  onClose,
  overlay,
}: PaySheetProps) {
  const styles = useThemedStyles(createPaySheetStyles);
  // An inactive account or category arrives empty, so the sheet opens with the fields to choose.
  const [expanded, setExpanded] = useState(defaultAccountId === null || defaultCategoryId === null);
  const [amount, setAmount] = useState(() => amountInputFromCents(amountCents));
  const [accountId, setAccountId] = useState(defaultAccountId);
  const [categoryId, setCategoryId] = useState(defaultCategoryId);
  const [date, setDate] = useState(today);
  const [tried, setTried] = useState(false);
  const [datePickerOpen, setDatePickerOpen] = useState(false);
  const title = `Pagar ${name}`;

  const parsed = parseAmountInput(amount);
  const cents = parsed.kind === 'ok' ? parsed.cents : 0;
  const amountValid = parsed.kind === 'ok' && parsed.cents > 0;
  const amountError = parsed.kind === 'negative' || (tried && !amountValid) ? PAY_AMOUNT_ERROR : undefined;
  const accountName = accounts.find((account) => account.id === accountId)?.name;
  const categoryName = categories.find((category) => category.value === categoryId)?.label;

  /** Pays, or says what is missing and shows the fields. */
  function confirm() {
    if (!amountValid || accountId === null || categoryId === null) {
      setTried(true);
      setExpanded(true);
      return;
    }
    onConfirm({ amountCents: cents, accountId, categoryId, operationDate: date });
  }

  /** Closes without paying, unless the payment is running: its result must reach the sheet. */
  function close() {
    if (!saving) {
      onClose();
    }
  }

  /** Picks a payment date chip; "Otra fecha…" opens the month sheet, also when already selected. */
  function pickDate(value: string | null) {
    if (value === OTHER_DATE || (value === null && dateChoice(today, dueDate, date) === OTHER_DATE)) {
      setDatePickerOpen(true);
    } else if (value !== null) {
      setDate(value);
    }
  }

  return (
    <BottomSheet
      label={title}
      onClose={close}
      overlay={
        <>
          {datePickerOpen && (
            <DatePickerSheet
              title="Fecha del pago"
              today={today}
              value={date}
              min={addDays(today, -PAY_DATE_MAX_DAYS_BACK)}
              max={today}
              // The design's legend names only "Hasta hoy" for a payment date.
              showMinInLegend={false}
              onConfirm={(picked) => {
                setDate(picked);
                setDatePickerOpen(false);
              }}
              onCancel={() => setDatePickerOpen(false)}
            />
          )}
          {overlay}
        </>
      }
    >
      <View style={styles.titleBlock}>
        <Text variant="heading" accessibilityRole="header">
          {title}
        </Text>
        <Text variant="body-sm" color="secondary">
          {dueText(today, dueDate)}
        </Text>
      </View>

      {!expanded ? (
        <View style={styles.compact}>
          <FactList tabular facts={paymentFacts({ today, amountCents: cents, accountName, categoryName, date })} />
          <View style={styles.expandLink}>
            <Button variant="ghost" size="sm" onPress={() => setExpanded(true)}>
              Cambiar monto, cuenta, categoría o fecha
            </Button>
          </View>
        </View>
      ) : (
        <>
          <AmountField
            label="Monto"
            value={amount}
            onChangeText={(text) => {
              setAmount(formatAmountInput(text));
              onDismissError();
            }}
            editable={!saving}
            error={amountError}
            help={amountValid && cents !== amountCents ? changedAmountHelp(amountCents) : undefined}
          />
          {(accounts.length > 1 || accountId === null) && (
            <ChoiceGroup
              label="Cuenta"
              options={accountOptions(accounts)}
              value={accountId}
              onChange={(value) => {
                setAccountId(value);
                onDismissError();
              }}
              error={tried && accountId === null ? PAY_ACCOUNT_ERROR : undefined}
            />
          )}
          <ChoiceGroup
            label="Categoría"
            options={categories}
            value={categoryId}
            onChange={setCategoryId}
            error={tried && categoryId === null ? PAY_CATEGORY_ERROR : undefined}
          />
          <View style={styles.dates}>
            <ChoiceGroup
              label="Fecha del pago"
              options={dateOptions(today, dueDate, date)}
              value={dateChoice(today, dueDate, date)}
              // Deselecting is how a second tap on the selected "Otra fecha…" reaches `pickDate`.
              allowDeselect
              onChange={pickDate}
            />
            <Text variant="body-sm" color="secondary">
              {PAY_DATE_HELP}
            </Text>
          </View>
        </>
      )}

      {error !== undefined && <Notice tone="danger" title={error.title} text={error.text} />}

      <View style={styles.actions}>
        <Button variant="ghost" size="lg" inactive={saving} onPress={close}>
          Cancelar
        </Button>
        <View style={styles.primarySlot}>
          <Button variant="primary" size="lg" fullWidth inactive={saving} onPress={confirm}>
            {`Pagar ${formatMoney(cents)}`}
          </Button>
        </View>
      </View>
    </BottomSheet>
  );
}
