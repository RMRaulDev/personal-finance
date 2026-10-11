import { useQuery } from '@tanstack/react-query';
import { useRef, useState } from 'react';
import { View, type ScrollView } from 'react-native';

import {
  accountsQuery,
  categoriesQuery,
  horizonQuery,
  isApiError,
  obligationOccurrencesQuery,
  obligationQuery,
  useArchiveObligation,
  useCancelOperation,
  usePayOccurrence,
  useReopenOccurrence,
  useSkipOccurrence,
  useSkipOverdueOccurrences,
  type IsoDate,
} from '../../../api';
import { Badge, Button, ConfirmDialog, Text, useThemedStyles } from '../../../design-system';
import { formatMoney } from '../../../lib';
import {
  ACCOUNTS_NOT_LOADED_ERROR,
  activeAccounts,
  BackHeader,
  CATEGORIES_NOT_LOADED_ERROR,
  categoryOptions,
  errorMessage,
  LoadState,
  Notice,
  Screen,
  SectionTitle,
  ToastOutlet,
  useDelayedLoading,
  useIsMounted,
  useMutationLock,
  useRefresh,
  useToast,
} from '../../../shared';
import { ArchivedNote } from '../components/ArchivedNote';
import { FactList } from '../components/FactList';
import { OverdueNotice } from '../components/OverdueNotice';
import { insufficientBalanceError, PaySheet, type PayError, type PaymentInput } from '../components/PaySheet';
import { ResolvedList, type ResolvedRowData } from '../components/ResolvedList';
import { UpcomingList } from '../components/UpcomingList';
import { blockedReason, frequencyText, loadFailureMessage } from '../obligations.model';

import {
  archivedText,
  archivedToastText,
  blockedText,
  detailFacts,
  dialogCopy,
  overdueSubtitle,
  overdueTitle,
  paidToastText,
  payOldestLabel,
  reopenedToastText,
  resolvedRows,
  skipAllLabel,
  skippedAllToastText,
  skippedToastText,
  undonePaymentToastText,
  upcomingRows,
  type DetailDialog,
} from './ObligationDetailScreen.model';
import { createObligationDetailScreenStyles } from './ObligationDetailScreen.styles';

/** Props of `ObligationDetailScreen`. */
export interface ObligationDetailScreenProps {
  obligationId: string;
  /** "‹ Compromisos", or "‹ Inicio" when the detail was opened from the dashboard. */
  backLabel: string;
  /** Goes back to the previous screen; also after archiving, with its toast. */
  onBack: () => void;
  /** Opens the edit form. */
  onEdit: () => void;
}

/** True for the 409 that says another device (or a previous attempt) already resolved the occurrence. */
function isAlreadyResolved(error: unknown): boolean {
  return isApiError(error) && error.status === 409 && error.code === 'OCCURRENCE_ALREADY_RESOLVED';
}

/** True for the 409 that says the account does not have enough balance. */
function isInsufficientBalance(error: unknown): boolean {
  return isApiError(error) && error.status === 409 && error.code === 'INSUFFICIENT_BALANCE';
}

/**
 * Detail of an obligation: its amount and frequency, what is overdue (with "Pagar la más antigua" and
 * "Saltar todas las vencidas"), the next payments with "Pagar" (opens the payment sheet) and "Saltar",
 * the latest resolutions with "Deshacer pago" or "Reabrir", its details, and "Editar" and "Archivar".
 * Paying and skipping offer "Deshacer" in the toast. An archived obligation is read-only.
 */
export function ObligationDetailScreen({ obligationId, backLabel, onBack, onEdit }: ObligationDetailScreenProps) {
  const styles = useThemedStyles(createObligationDetailScreenStyles);
  const obligation = useQuery(obligationQuery(obligationId));
  const occurrences = useQuery(obligationOccurrencesQuery(obligationId));
  // "Today" is the server's: the dashboard's horizon, already cached by the dashboard.
  const horizon = useQuery(horizonQuery());
  // The payment sheet's choices, and the statuses that say whether a payment is blocked.
  const accounts = useQuery(accountsQuery());
  const categories = useQuery(categoriesQuery());
  const pay = usePayOccurrence();
  const skip = useSkipOccurrence();
  const reopen = useReopenOccurrence();
  const skipOverdue = useSkipOverdueOccurrences();
  const cancel = useCancelOperation();
  const archive = useArchiveObligation();
  const { show } = useToast();
  // One payment, skip, undo, or archive at a time; their buttons stay inactive meanwhile.
  const lock = useMutationLock();
  const isMounted = useIsMounted();
  const [payDueDate, setPayDueDate] = useState<IsoDate | null>(null);
  const [payError, setPayError] = useState<PayError | undefined>(undefined);
  const [payAlreadyResolved, setPayAlreadyResolved] = useState(false);
  const [dialog, setDialog] = useState<DetailDialog | null>(null);
  const scrollRef = useRef<ScrollView>(null);
  // Where the overdue block starts in the scrolling content, for "Ver vencidos".
  const overdueOffset = useRef(0);
  const { refreshing, refresh } = useRefresh(
    [obligation.refetch, occurrences.refetch, horizon.refetch, accounts.refetch, categories.refetch],
    (error) => show(errorMessage(error)),
  );
  // The account and category lists are waited for too: without their statuses the payment sheet
  // has no choices and the blocked notice cannot be decided.
  const pending =
    obligation.isPending || occurrences.isPending || horizon.isPending || accounts.isPending || categories.isPending;
  const loadingVisible = useDelayedLoading(pending);

  if (pending || loadingVisible) {
    return (
      <LoadState backLabel={backLabel} onBack={onBack} loading={loadingVisible} loadingText="Cargando compromiso…" />
    );
  }

  // Only without data: a failed refetch keeps the last detail on screen and shows a toast.
  if (obligation.data === undefined || occurrences.data === undefined || horizon.data === undefined) {
    return (
      <LoadState
        backLabel={backLabel}
        onBack={onBack}
        loading={false}
        loadingText="Cargando compromiso…"
        failure={{
          title: 'No pudimos cargar este compromiso',
          message: loadFailureMessage(obligation.error ?? occurrences.error ?? horizon.error),
          onRetry: () => {
            void obligation.refetch();
            void occurrences.refetch();
            void horizon.refetch();
          },
        }}
      />
    );
  }

  const data = obligation.data;
  const today = horizon.data.from;
  const archived = data.status === 'ARCHIVED';
  const name = data.name;
  const overdueDates = occurrences.data.overdue.map((occurrence) => occurrence.dueDate);
  const upcoming = upcomingRows(occurrences.data, data.amountCents, today);
  const resolved = resolvedRows(occurrences.data, today);
  // A failed load of the accounts or categories leaves the detail usable, but their statuses are
  // unknown: the blocked notice is not decided (neither shown nor ruled out) and the payment sheet
  // says what is missing instead of offering empty choices.
  const listsError =
    accounts.data === undefined
      ? ACCOUNTS_NOT_LOADED_ERROR
      : categories.data === undefined
        ? CATEGORIES_NOT_LOADED_ERROR
        : undefined;
  const blocked =
    archived || accounts.data === undefined || categories.data === undefined
      ? null
      : blockedReason(data, accounts.data, categories.data);
  const payAccounts = activeAccounts(accounts.data ?? []);
  const payCategories = categoryOptions(categories.data ?? [], 'EXPENSE');

  /** Shows a failure in the app's toast, with the shared error copy. */
  function showError(error: unknown) {
    show(errorMessage(error));
  }

  /**
   * Runs a mutation under the lock, then `onSuccess` with its result or `onError` (the app's toast
   * by default), and always releases the lock. `mutateAsync` instead of `mutate`: TanStack Query
   * drops the per-call callbacks of `mutate` once the screen unmounts, so a request that settles
   * after the user left (or a "Deshacer" pressed in a toast that outlived the detail; the toast is
   * the app's, shown on whatever screen is focused) would lose its toast and keep the lock held.
   * The callbacks check `isMounted()` before touching this screen's state.
   */
  function runMutation<T>(
    request: () => Promise<T>,
    onSuccess: (result: T) => void,
    onError: (error: unknown) => void = showError,
  ) {
    lock.run((release) => {
      void (async () => {
        try {
          const result = await request();
          onSuccess(result);
        } catch (error) {
          onError(error);
        } finally {
          release();
        }
      })();
    });
  }

  /**
   * A skip that found the date already resolved: the dialog while the detail is on screen, the
   * app's toast once it is gone.
   */
  function showSkipError(error: unknown) {
    if (isAlreadyResolved(error) && isMounted()) {
      setDialog('alreadyResolved');
    } else {
      showError(error);
    }
  }

  /**
   * Undoes a payment by cancelling its expense; `confirm` adds the "Deshiciste…" toast. Also called
   * from the toast's "Deshacer", which may outlive this screen.
   */
  function undoPayment(operationId: string, confirm: boolean) {
    runMutation(
      () => cancel.mutateAsync(operationId),
      () => {
        if (confirm) {
          show(undonePaymentToastText(name));
        }
      },
    );
  }

  /**
   * Reopens a skipped date; `confirm` adds the "Reabriste…" toast. Also called from the toast's
   * "Deshacer", which may outlive this screen.
   */
  function reopenDate(dueDate: IsoDate, confirm: boolean) {
    runMutation(
      () => reopen.mutateAsync({ obligationId, dueDate }),
      () => {
        if (confirm) {
          show(reopenedToastText(name, dueDate, today));
        }
      },
    );
  }

  /**
   * Undoes "skip all" by reopening each skipped date, one after the other, under the lock. If one
   * fails, the rest are left skipped and the error is shown; they can still be reopened from
   * "Resueltos".
   */
  function reopenDates(dueDates: IsoDate[]) {
    runMutation(
      async () => {
        for (const dueDate of dueDates) {
          await reopen.mutateAsync({ obligationId, dueDate });
        }
      },
      () => undefined,
    );
  }

  /** Skips one date right away and offers to undo it. */
  function skipDate(dueDate: IsoDate) {
    runMutation(
      () => skip.mutateAsync({ obligationId, dueDate }),
      () =>
        show(skippedToastText(name, dueDate, today), {
          label: 'Deshacer',
          onPress: () => reopenDate(dueDate, false),
        }),
      showSkipError,
    );
  }

  /** Skips every overdue date (after the dialog) and offers to undo it. */
  function skipAllOverdue() {
    runMutation(
      () => skipOverdue.mutateAsync(obligationId),
      ({ skippedDueDates }) => {
        if (skippedDueDates.length > 0) {
          show(skippedAllToastText(name, skippedDueDates.length), {
            label: 'Deshacer',
            onPress: () => reopenDates(skippedDueDates),
          });
        }
      },
    );
  }

  /** Archives (after the dialog), goes back, and says so there. */
  function archiveObligation() {
    runMutation(
      () => archive.mutateAsync(obligationId),
      () => {
        show(archivedToastText(name));
        // Unless the user already left the detail meanwhile: going back again would leave the
        // screen they went to.
        if (isMounted()) {
          onBack();
        }
      },
    );
  }

  /**
   * Opens the payment sheet for a date, without the last sheet's error. If the accounts or
   * categories failed to load, opening it retries them, which is the "Intenta de nuevo" of the
   * sheet's error.
   */
  function openPayment(dueDate: IsoDate) {
    if (accounts.isError) {
      void accounts.refetch();
    }
    if (categories.isError) {
      void categories.refetch();
    }
    setPayError(undefined);
    setPayDueDate(dueDate);
  }

  /** What the payment sheet shows when a payment fails. */
  function paymentError(error: unknown, payment: PaymentInput): PayError {
    const account = accounts.data?.find((item) => item.id === payment.accountId);
    if (isInsufficientBalance(error) && account !== undefined) {
      return insufficientBalanceError(account.name, account.balanceCents, payment.amountCents);
    }
    return { text: errorMessage(error) };
  }

  /**
   * Pays with what the sheet hands over; success closes it and offers to undo the payment. Failures
   * show in the sheet while the detail is on screen, in the app's toast once it is gone.
   */
  function confirmPayment(payment: PaymentInput) {
    if (payDueDate === null) {
      return;
    }
    const dueDate = payDueDate;
    setPayError(undefined);
    runMutation(
      () => pay.mutateAsync({ obligationId, dueDate, body: payment }),
      ({ operationId }) => {
        if (isMounted()) {
          setPayDueDate(null);
        }
        show(paidToastText(name, payment.amountCents), {
          label: 'Deshacer',
          onPress: () => undoPayment(operationId, false),
        });
      },
      (error) => {
        if (!isMounted()) {
          showError(error);
        } else if (isAlreadyResolved(error)) {
          // Shown over the sheet; the detail behind it is already being refreshed.
          setPayAlreadyResolved(true);
        } else {
          setPayError(paymentError(error, payment));
        }
      },
    );
  }

  /** "Deshacer pago" or "Reabrir" from "Resueltos". */
  function revert(row: ResolvedRowData) {
    if (row.action.kind === 'undoPayment') {
      undoPayment(row.action.operationId, true);
    } else {
      reopenDate(row.action.dueDate, true);
    }
  }

  /** Runs the confirmed action of the open dialog and closes it. */
  function confirmDialog(open: DetailDialog) {
    setDialog(null);
    if (open === 'archive') {
      archiveObligation();
    } else if (open === 'skipAll') {
      skipAllOverdue();
    } else if (open === 'archiveBlocked') {
      // "Ver vencidos" leaves the overdue block in view.
      scrollRef.current?.scrollTo({ y: overdueOffset.current, animated: true });
    }
  }

  const accountActive = payAccounts.some((account) => account.id === data.account.id);
  const categoryActive = payCategories.some((category) => category.value === data.category.id);

  return (
    <Screen
      header={<BackHeader label={backLabel} onPress={onBack} />}
      refreshing={refreshing}
      onRefresh={refresh}
      scrollRef={scrollRef}
      overlay={
        <>
          <ToastOutlet />
          {payDueDate !== null && (
            <PaySheet
              name={name}
              amountCents={data.amountCents}
              dueDate={payDueDate}
              today={today}
              accounts={payAccounts}
              defaultAccountId={accountActive ? data.account.id : null}
              categories={payCategories}
              defaultCategoryId={categoryActive ? data.category.id : null}
              saving={lock.locked}
              // A failed payment's error first; else what is missing to pay at all.
              error={payError ?? (listsError !== undefined ? { text: listsError } : undefined)}
              onDismissError={() => setPayError(undefined)}
              onConfirm={confirmPayment}
              onClose={() => setPayDueDate(null)}
              overlay={
                payAlreadyResolved && (
                  <ConfirmDialog
                    {...dialogCopy('alreadyResolved', name, overdueDates, data.overdueCount, today)}
                    onConfirm={() => {
                      setPayAlreadyResolved(false);
                      setPayDueDate(null);
                    }}
                    onCancel={() => {
                      setPayAlreadyResolved(false);
                      setPayDueDate(null);
                    }}
                  />
                )
              }
            />
          )}
          {dialog !== null && (
            <ConfirmDialog
              {...dialogCopy(dialog, name, overdueDates, data.overdueCount, today)}
              onConfirm={() => confirmDialog(dialog)}
              onCancel={() => setDialog(null)}
            />
          )}
        </>
      }
    >
      <View style={styles.titleBlock}>
        <View style={styles.nameRow}>
          <Text variant="title-2" accessibilityRole="header">
            {name}
          </Text>
          {archived && <Badge>Archivado</Badge>}
        </View>
        <Text variant="amount" tabular>
          {formatMoney(data.amountCents)}
        </Text>
        <Text color="secondary">{frequencyText(data, today)}</Text>
      </View>

      {archived && <ArchivedNote text={archivedText(data, today)} />}

      {blocked !== null && (
        <Notice title="Pago bloqueado" text={blockedText(blocked)}>
          <Button fullWidth onPress={onEdit}>
            Editar compromiso
          </Button>
        </Notice>
      )}

      {overdueDates.length > 0 && (
        <View
          onLayout={(event) => {
            overdueOffset.current = event.nativeEvent.layout.y;
          }}
        >
          <OverdueNotice
            title={overdueTitle(overdueDates.length)}
            subtitle={overdueSubtitle(today, overdueDates, data.amountCents)}
            payLabel={payOldestLabel(today, overdueDates)}
            skipLabel={skipAllLabel(overdueDates.length)}
            busy={lock.locked}
            onPayOldest={() => {
              const oldest = overdueDates[0];
              if (oldest !== undefined) {
                openPayment(oldest);
              }
            }}
            onSkipAll={() => setDialog('skipAll')}
          />
        </View>
      )}

      {!archived && upcoming.length > 0 && (
        <UpcomingList
          title={overdueDates.length > 0 ? 'Por resolver' : 'Próximos'}
          rows={upcoming}
          busy={lock.locked}
          onPay={openPayment}
          onSkip={skipDate}
        />
      )}

      {resolved.length > 0 && <ResolvedList rows={resolved} canAct={!archived} busy={lock.locked} onAction={revert} />}

      <View style={styles.section}>
        <SectionTitle>Detalles</SectionTitle>
        <FactList facts={detailFacts(data, categories.data, today)} />
      </View>

      {!archived && (
        <View style={styles.manage}>
          <Button size="lg" fullWidth onPress={onEdit}>
            Editar
          </Button>
          <Button
            variant="ghost"
            size="lg"
            fullWidth
            onPress={() => setDialog(data.overdueCount > 0 ? 'archiveBlocked' : 'archive')}
          >
            Archivar
          </Button>
        </View>
      )}
    </Screen>
  );
}
