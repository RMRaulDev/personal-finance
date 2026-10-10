import { useMutation, useQueryClient } from '@tanstack/react-query';

import { invalidateMoneyMovement } from '../../client';

import { cancelOperation, registerExpense, registerIncome } from './endpoints';

// Mutations of the operations resource. Every one moves money, so all share one invalidation.

/** Registers an expense and refreshes every query a money movement changes. Never retried. */
export function useRegisterExpense() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: registerExpense,
    retry: 0,
    onSuccess: () => invalidateMoneyMovement(queryClient),
  });
}

/** Registers an income and refreshes every query a money movement changes. Never retried. */
export function useRegisterIncome() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: registerIncome,
    retry: 0,
    onSuccess: () => invalidateMoneyMovement(queryClient),
  });
}

/**
 * Cancels an operation (e.g. to undo a payment) and refreshes every query a money movement
 * changes. Never retried.
 */
export function useCancelOperation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (operationId: string) => cancelOperation(operationId),
    retry: 0,
    onSuccess: () => invalidateMoneyMovement(queryClient),
  });
}
