import { useState } from 'react';
import { View } from 'react-native';

import {
  useCreateObligation,
  useModifyObligation,
  type AccountResponse,
  type CategoryResponse,
  type Horizon,
  type ObligationOccurrencesResponse,
  type ObligationResponse,
} from '../../../api';
import {
  AmountField,
  Button,
  ChoiceGroup,
  DateField,
  DatePickerSheet,
  Field,
  space,
  Text,
  useThemedStyles,
} from '../../../design-system';
import { formatAmountInput } from '../../../lib';
import {
  accountOptions,
  activeAccounts,
  BackHeader,
  BottomBar,
  categoryOptions,
  DetailRow,
  errorMessage,
  Notice,
  Screen,
  ToastOutlet,
  useIsMounted,
  useMutationLock,
  useToast,
} from '../../../shared';
import { FactList } from '../components/FactList';
import { SummaryBox } from '../components/SummaryBox';

import { createObligationFormStyles } from './ObligationForm.styles';
import {
  createdToastText,
  createRequest,
  dateText,
  END_OPTIONS,
  endHelp,
  FREQUENCY_OPTIONS,
  initialValues,
  isValid,
  lockedFacts,
  lockText,
  minStartDate,
  modifyRequest,
  NAME_MAX_LENGTH,
  quickStartDates,
  SAVED_TOAST_TEXT,
  saveFailure,
  startHelp,
  startLabel,
  summaryText,
  validate,
  type FormErrors,
  type FormNotice,
  type FormValues,
} from './ObligationFormScreen.model';

/** Props of `ObligationForm`. */
export interface ObligationFormProps {
  /** The obligation to edit; undefined to create one. */
  editing: ObligationResponse | undefined;
  /** Its occurrences, to name an advance payment in a calendar conflict; optional. */
  occurrences: ObligationOccurrencesResponse | undefined;
  /** Every obligation, to catch a repeated name before saving; undefined if it did not load. */
  obligations: ObligationResponse[] | undefined;
  /** Every account; only the active ones are offered. */
  accounts: AccountResponse[];
  /** Every category; only the active expense ones are offered. */
  categories: CategoryResponse[];
  /** The dashboard's horizon: `from` is the server's today. */
  horizon: Horizon;
  /** "Cancelar": back without saving. */
  onCancel: () => void;
  /** Back after saving; the toast is already shown. */
  onDone: () => void;
  /** "Resolver vencidos": back to the detail, where the overdue block is. */
  onResolveOverdue: () => void;
}

/** Which date the month sheet is choosing, or null while it is closed. */
type DateSheet = 'start' | 'end' | null;

/**
 * The obligation form, once its data is loaded: name, amount, frequency (monthly preselected), first
 * payment with quick dates, optional end, account, and category, with a summary in plain language
 * and "Guardar". Editing an obligation with overdue payments only allows the name. Client checks
 * mirror the server's rules; server errors show next to the field they concern.
 */
export function ObligationForm({
  editing,
  occurrences,
  obligations,
  accounts,
  categories,
  horizon,
  onCancel,
  onDone,
  onResolveOverdue,
}: ObligationFormProps) {
  const styles = useThemedStyles(createObligationFormStyles);
  const create = useCreateObligation();
  const modify = useModifyObligation();
  const lock = useMutationLock();
  const isMounted = useIsMounted();
  const { show } = useToast();
  const active = activeAccounts(accounts);
  const [values, setValues] = useState<FormValues>(() => initialValues(editing, accounts, categories));
  const [errors, setErrors] = useState<FormErrors>({});
  const [notice, setNotice] = useState<FormNotice | undefined>(undefined);
  const [dateSheet, setDateSheet] = useState<DateSheet>(null);
  const today = horizon.from;
  const creating = editing === undefined;
  // Every edit but the name waits until the overdue payments are paid or skipped.
  const locked = editing !== undefined && editing.overdueCount > 0;
  const summary = locked ? '' : summaryText(values, today, creating);

  /** Applies a change, clearing the errors it answers and the failure notice. */
  function update(changes: Partial<FormValues>, cleared: (keyof FormErrors)[] = []) {
    setValues((current) => ({ ...current, ...changes }));
    if (cleared.length > 0) {
      setErrors((current) => {
        const next = { ...current };
        for (const key of cleared) {
          delete next[key];
        }
        return next;
      });
    }
    setNotice(undefined);
  }

  /** Shows why the save failed, keeping everything typed. */
  function showFailure(error: unknown) {
    const failure = saveFailure(error, { values, today, editing, occurrences });
    setErrors((current) => ({ ...current, ...failure.errors }));
    setNotice(failure.notice);
  }

  /** Goes back after a save, unless the user already left the form meanwhile. */
  function finish(toast: string) {
    show(toast);
    if (isMounted()) {
      onDone();
    }
  }

  /**
   * Runs a save under the lock. `mutateAsync` instead of `mutate`: TanStack Query drops the
   * per-call callbacks of `mutate` once the form unmounts, so a save that settles after the user
   * left would lose its toast and keep the lock. The toast is the app's, so it still shows; the
   * form's own state is only touched while it is mounted.
   */
  function runSave(request: () => Promise<unknown>, toast: string) {
    lock.run((release) => {
      void (async () => {
        try {
          await request();
          finish(toast);
        } catch (error) {
          if (isMounted()) {
            showFailure(error);
          } else {
            show(errorMessage(error));
          }
        } finally {
          release();
        }
      })();
    });
  }

  /** "Guardar": checks the form, then creates or changes only what changed. */
  function save() {
    const found = validate(values, { today, editing, obligations, locked });
    setErrors(found);
    setNotice(undefined);
    if (!isValid(found)) {
      return;
    }
    if (editing === undefined) {
      const body = createRequest(values);
      if (body === null) {
        return;
      }
      runSave(() => create.mutateAsync(body), createdToastText(body.name, body.recurrence.startDate, horizon));
      return;
    }
    const changes = modifyRequest(values, editing, locked);
    if (Object.keys(changes).length === 0) {
      // Nothing changed: the API rejects an empty change, and there is nothing to save.
      finish(SAVED_TOAST_TEXT);
      return;
    }
    runSave(() => modify.mutateAsync({ obligationId: editing.id, changes }), SAVED_TOAST_TEXT);
  }

  /** Closes the month sheet; dismissing it from "Hasta una fecha" without a date drops the end. */
  function cancelDateSheet() {
    if (dateSheet === 'end' && values.endDate === null) {
      update({ hasEnd: false });
    }
    setDateSheet(null);
  }

  // Never the obligation's own account as a fallback: it may be the inactive one being replaced.
  const accountName = accounts.find((account) => account.id === values.accountId)?.name ?? '—';

  return (
    <Screen
      header={<BackHeader label="Cancelar" onPress={onCancel} />}
      // No pull-to-refresh: what is typed must not be replaced.
      overlay={
        <>
          <ToastOutlet bottom={space[5]} />
          {dateSheet === 'start' && (
            <DatePickerSheet
              title={startLabel(values.frequency)}
              today={today}
              value={values.startDate}
              min={minStartDate(today, !creating)}
              onConfirm={(picked) => {
                update({ startDate: picked }, ['start', 'end']);
                setDateSheet(null);
              }}
              onCancel={cancelDateSheet}
            />
          )}
          {dateSheet === 'end' && (
            <DatePickerSheet
              title="Último pago"
              today={today}
              value={values.endDate}
              initialMonth={values.endDate ?? values.startDate ?? today}
              min={values.startDate ?? today}
              onConfirm={(picked) => {
                update({ endDate: picked, hasEnd: true }, ['end']);
                setDateSheet(null);
              }}
              onCancel={cancelDateSheet}
            />
          )}
        </>
      }
      footer={
        <BottomBar>
          <View style={styles.primarySlot}>
            <Button variant="primary" size="lg" fullWidth inactive={lock.locked} onPress={save}>
              {creating ? 'Guardar compromiso' : 'Guardar cambios'}
            </Button>
          </View>
        </BottomBar>
      }
    >
      <View style={styles.content}>
        <Text variant="title-2" accessibilityRole="header">
          {editing === undefined ? 'Nuevo compromiso' : `Editar ${editing.name}`}
        </Text>

        {locked && editing !== undefined && (
          <Notice tone="warning" text={lockText(editing.name, editing.overdueCount)}>
            <Button fullWidth onPress={onResolveOverdue}>
              Resolver vencidos
            </Button>
          </Notice>
        )}

        <Field
          label="Nombre"
          value={values.name}
          onChangeText={(name) => update({ name }, ['name'])}
          placeholder="Renta, internet, gimnasio…"
          maxLength={NAME_MAX_LENGTH}
          size="lg"
          fullWidth
          editable={!lock.locked}
          error={errors.name}
        />

        {!locked && (
          <>
            <AmountField
              label="Monto de cada pago"
              value={values.amount}
              onChangeText={(text) => update({ amount: formatAmountInput(text) }, ['amount'])}
              editable={!lock.locked}
              error={errors.amount}
            />

            <ChoiceGroup
              label="Frecuencia"
              options={FREQUENCY_OPTIONS}
              value={values.frequency}
              onChange={(frequency) => {
                if (frequency !== null) {
                  // A single payment has no end.
                  update(frequency === 'ONCE' ? { frequency, hasEnd: false } : { frequency });
                }
              }}
            />

            <DateField
              label={startLabel(values.frequency)}
              text={dateText(today, values.startDate)}
              placeholder="Elige una fecha"
              onPress={() => setDateSheet('start')}
              quickDates={quickStartDates(today)}
              value={values.startDate}
              onQuickPick={(startDate) => update({ startDate }, ['start', 'end'])}
              help={startHelp(!creating)}
              error={errors.start}
            />

            {values.frequency !== 'ONCE' && (
              <View style={styles.group}>
                <ChoiceGroup
                  label="Fin"
                  options={END_OPTIONS}
                  value={values.hasEnd ? 'date' : 'none'}
                  onChange={(mode) => {
                    if (mode === 'date') {
                      update({ hasEnd: true }, ['end']);
                      // Choosing "Hasta una fecha" without one goes straight to the month sheet.
                      if (values.endDate === null) {
                        setDateSheet('end');
                      }
                    } else if (mode === 'none') {
                      update({ hasEnd: false }, ['end']);
                    }
                  }}
                />
                {values.hasEnd ? (
                  <DateField
                    label="Último pago"
                    labelHidden
                    text={values.endDate === null ? undefined : `Último pago: ${dateText(today, values.endDate)}`}
                    placeholder="Elige el último pago"
                    onPress={() => setDateSheet('end')}
                    help={endHelp(true)}
                    error={errors.end}
                  />
                ) : (
                  <Text variant="body-sm" color="secondary">
                    {endHelp(false)}
                  </Text>
                )}
              </View>
            )}

            {active.length === 1 ? (
              // The only active account is a fact, not a choice: shown between dividers.
              <View style={styles.accountFact}>
                <DetailRow label="Se paga desde" value={accountName} />
              </View>
            ) : (
              <ChoiceGroup
                label="Se paga desde"
                options={accountOptions(active)}
                value={values.accountId}
                onChange={(accountId) => update({ accountId }, ['account'])}
                error={errors.account}
              />
            )}

            <ChoiceGroup
              label="Categoría"
              options={categoryOptions(categories, 'EXPENSE')}
              value={values.categoryId}
              onChange={(categoryId) => update({ categoryId }, ['category'])}
              error={errors.category}
            />
          </>
        )}

        {locked && editing !== undefined && <FactList muted facts={lockedFacts(editing, today)} />}

        {summary !== '' && <SummaryBox text={summary} />}

        {notice !== undefined && <Notice tone="danger" title={notice.title} text={notice.text} />}
      </View>
    </Screen>
  );
}
