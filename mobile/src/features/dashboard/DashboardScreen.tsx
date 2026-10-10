import { useQuery } from '@tanstack/react-query';
import { View } from 'react-native';

import { dashboardQuery, obligationsQuery, useCancelOperation, usePayOccurrence } from '../../api';
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

import { AccountNotice } from './AccountNotice';
import { AvailableSummary } from './AvailableSummary';
import { CommitmentList } from './CommitmentList';
import {
  accountNoticeText,
  accountShortfalls,
  commitmentRows,
  extraNoticesText,
  hasAnyObligation,
  horizonDays,
  horizonLabel,
  paidToastText,
  recentOperations,
  type CommitmentRow,
} from './model';
import { RecentList } from './RecentList';

/**
 * Home screen ("¿qué hago ahora?"): how much is available to spend, the commitments due in the
 * horizon with a "Pagar" action, and the latest operations. Registration (Ingreso, + Gasto,
 * Transferir) is not shown until its sheet is redesigned.
 */
export function DashboardScreen() {
  const dashboard = useQuery(dashboardQuery());
  const hasObligations = useQuery({ ...obligationsQuery(), select: hasAnyObligation });
  const pay = usePayOccurrence();
  const cancel = useCancelOperation();
  const { toast, show, hide } = useToast();
  // Only one payment or undo runs at a time; every "Pagar" stays inactive meanwhile.
  const lock = useMutationLock();
  const { refreshing, refresh } = useRefresh([dashboard.refetch, hasObligations.refetch], (error) =>
    show(errorMessage(error)),
  );

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
      overlay={toast && <Toast toast={toast} onDismiss={hide} />}
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
