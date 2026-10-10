import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { View } from 'react-native';

import {
  accountsQuery,
  categoriesQuery,
  dashboardQuery,
  obligationsQuery,
  useCancelOperation,
  usePayOccurrence,
  useRegisterExpense,
  useRegisterIncome,
} from '../../api';
import { space, Text } from '../../design-system';
import { formatLongDate } from '../../lib';
import {
  errorMessage,
  Screen,
  ScreenError,
  ScreenLoading,
  Toast,
  useMutationLock,
  useRefresh,
  useToast,
} from '../../shared';

import { AccountNotice } from './components/AccountNotice/AccountNotice';
import { ActionBar } from './components/ActionBar/ActionBar';
import { AvailableSummary } from './components/AvailableSummary/AvailableSummary';
import { CommitmentList } from './components/CommitmentList/CommitmentList';
import {
  ACCOUNTS_NOT_LOADED_ERROR,
  accountNoticeText,
  accountShortfalls,
  activeAccounts,
  arrivalToastText,
  categoryOptions,
  commitmentRows,
  defaultAccountId,
  extraNoticesText,
  hasAnyObligation,
  NO_ACTIVE_ACCOUNT_ERROR,
  horizonDays,
  horizonLabel,
  paidToastText,
  recentOperations,
  registeredToastText,
  type CommitmentRow,
  type RegistrationType,
} from './dashboard.model';
import { RecentList } from './components/RecentList/RecentList';
import { RegistrationSheet, type RegistrationInput } from './components/RegistrationSheet/RegistrationSheet';

/** Props of `DashboardScreen`. */
export interface DashboardScreenProps {
  /**
   * Name of the account the onboarding just created, when the dashboard opens right after it: a
   * toast says "{nombre} está lista." once, when the data first shows.
   */
  arrivalAccountName?: string;
  /**
   * Called right after the arrival toast is shown, so the route can drop its param and a remount
   * (e.g. after the app is restored) does not show it again.
   */
  onArrivalShown?: () => void;
}

/**
 * Home screen ("¿qué hago ahora?"): how much is available to spend, the commitments due in the
 * horizon with a "Pagar" action, the latest operations, and the bottom bar that opens the
 * registration sheet ("Ingreso", "+ Gasto"). "Transferir" stays hidden until it has a design.
 */
export function DashboardScreen({ arrivalAccountName, onArrivalShown }: DashboardScreenProps) {
  const dashboard = useQuery(dashboardQuery());
  const hasObligations = useQuery({ ...obligationsQuery(), select: hasAnyObligation });
  // Loaded with the dashboard so the sheet opens with its choices ready.
  const accounts = useQuery(accountsQuery());
  const categories = useQuery(categoriesQuery());
  const pay = usePayOccurrence();
  const cancel = useCancelOperation();
  const registerExpense = useRegisterExpense();
  const registerIncome = useRegisterIncome();
  const { toast, show, hide } = useToast();
  // Only one payment, undo, or registration runs at a time; their buttons stay inactive meanwhile.
  const lock = useMutationLock();
  const [sheet, setSheet] = useState<RegistrationType | null>(null);
  const [sheetError, setSheetError] = useState<string | undefined>(undefined);
  const { refreshing, refresh } = useRefresh(
    [dashboard.refetch, hasObligations.refetch, accounts.refetch, categories.refetch],
    (error) => show(errorMessage(error)),
  );

  const hasData = dashboard.data !== undefined;
  const arrivalShown = useRef(false);
  useEffect(() => {
    // Once the dashboard has data, so the toast does not spend its time behind the loading screen.
    if (arrivalAccountName && hasData && !arrivalShown.current) {
      arrivalShown.current = true;
      show(arrivalToastText(arrivalAccountName));
      onArrivalShown?.();
    }
  }, [arrivalAccountName, hasData, show, onArrivalShown]);

  /** Opens the registration sheet, empty. */
  function openSheet(type: RegistrationType) {
    setSheetError(undefined);
    setSheet(type);
  }

  /**
   * Registers what the sheet hands over, dated the server's today. Success closes the sheet and
   * shows a toast; a failure keeps it open with the error under the amount.
   */
  function register(input: RegistrationInput, today: string) {
    const { accountId } = input;
    if (accountId === undefined) {
      // No account to register in: say why instead of sending a request.
      if (accounts.data !== undefined) {
        setSheetError(NO_ACTIVE_ACCOUNT_ERROR);
      } else if (accounts.isError) {
        setSheetError(errorMessage(accounts.error));
      } else {
        setSheetError(ACCOUNTS_NOT_LOADED_ERROR);
      }
      return;
    }
    const mutation = input.type === 'EXPENSE' ? registerExpense : registerIncome;
    lock.run((release) => {
      setSheetError(undefined);
      mutation.mutate(
        {
          accountId,
          categoryId: input.categoryId ?? undefined,
          amountCents: input.amountCents,
          operationDate: today,
        },
        {
          onSuccess: () => {
            setSheet(null);
            show(registeredToastText(input.type, input.amountCents));
          },
          onError: (error) => setSheetError(errorMessage(error)),
          onSettled: release,
        },
      );
    });
  }

  /** Undoes a payment by cancelling the expense it registered. */
  function undoPayment(operationId: string) {
    lock.run((release) =>
      cancel.mutate(operationId, {
        onSuccess: () => show('Pago deshecho.'),
        onError: (error) => show(errorMessage(error)),
        onSettled: release,
      }),
    );
  }

  /** Pays one occurrence and offers to undo it. */
  function payRow(row: CommitmentRow) {
    lock.run((release) =>
      pay.mutate(
        { obligationId: row.obligationId, dueDate: row.dueDate },
        {
          onSuccess: ({ operationId }) =>
            show(paidToastText(row), {
              label: 'Deshacer',
              onPress: () => undoPayment(operationId),
            }),
          onError: (error) => show(errorMessage(error)),
          onSettled: release,
        },
      ),
    );
  }

  if (dashboard.isPending) {
    return <ScreenLoading />;
  }

  // Only without data: a failed refetch keeps the last dashboard on screen (and any open toast).
  if (dashboard.data === undefined) {
    return <ScreenError message={errorMessage(dashboard.error)} onRetry={() => void dashboard.refetch()} />;
  }

  const data = dashboard.data;
  const days = horizonDays(data);
  const notices = accountShortfalls(data);
  const [firstNotice, ...extraNotices] = notices;
  const sheetAccounts = activeAccounts(accounts.data ?? []);

  return (
    <Screen
      refreshing={refreshing}
      onRefresh={refresh}
      header={
        <View style={{ paddingTop: space[2], paddingRight: space[4], paddingLeft: space[5] }}>
          {/* "Today" is the server's, not the device's: both must agree on what is due. */}
          <Text variant="body-sm" color="secondary">
            {formatLongDate(data.horizon.from)}
          </Text>
        </View>
      }
      overlay={
        <>
          {/* 20px above the action bar: the design's 112px from the bottom minus its 92px bar. */}
          {toast && <Toast toast={toast} onDismiss={hide} bottom={space[5]} />}
          {sheet && (
            <RegistrationSheet
              type={sheet}
              categories={categoryOptions(categories.data ?? [], sheet)}
              accounts={sheetAccounts}
              defaultAccountId={defaultAccountId(data.recent, sheetAccounts)}
              saving={lock.locked}
              serverError={sheetError}
              onDismissError={() => setSheetError(undefined)}
              // "Today" is the server's: the operation is dated the day the dashboard shows.
              onSave={(input) => register(input, data.horizon.from)}
              onClose={() => setSheet(null)}
            />
          )}
        </>
      }
      footer={<ActionBar onIncome={() => openSheet('INCOME')} onExpense={() => openSheet('EXPENSE')} />}
    >
      <AvailableSummary availableToSpend={data.availableToSpend} horizonLabel={horizonLabel(data)} />

      {firstNotice && (
        <AccountNotice
          text={accountNoticeText(firstNotice, days)}
          extraCount={extraNotices.length}
          onShowExtra={() => show(extraNoticesText(extraNotices))}
        />
      )}

      <CommitmentList
        rows={commitmentRows(data)}
        committedCents={data.availableToSpend.committedCents}
        days={days}
        // Until the obligations load, assume they exist so the new-user invite does not flash.
        hasObligations={hasObligations.data ?? true}
        payPending={lock.locked}
        onPay={payRow}
      />

      <RecentList operations={recentOperations(data)} today={data.horizon.from} />
    </Screen>
  );
}
