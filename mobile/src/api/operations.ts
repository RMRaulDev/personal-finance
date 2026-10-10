import { useMutation, useQueryClient } from '@tanstack/react-query';

import { request } from './client/client';
import type { IsoDate } from './resources/common/types';
import { invalidateMoneyMovement } from './client/invalidation';

// The operations resource: `/api/v1/operations` (`entry.web.operation`).

/**
 * Body of `POST /operations/incomes` and `/expenses`. `categoryId` is optional: without it the
 * operation is registered without a category. `operationDate` should be the server's today.
 */
export interface RegisterOperationRequest {
  accountId: string;
  categoryId?: string;
  amountCents: number;
  operationDate: IsoDate;
}

/** Response of a registration: the id of the new operation. */
export interface RegisterOperationResponse {
  id: string;
}

/** `POST /operations/expenses`: registers an expense. Not idempotent: a retry registers it twice. */
export function registerExpense(body: RegisterOperationRequest): Promise<RegisterOperationResponse> {
  return request<RegisterOperationResponse>('POST', '/operations/expenses', body);
}

/** `POST /operations/incomes`: registers an income. Not idempotent: a retry registers it twice. */
export function registerIncome(body: RegisterOperationRequest): Promise<RegisterOperationResponse> {
  return request<RegisterOperationResponse>('POST', '/operations/incomes', body);
}

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

/** `POST /operations/{id}/cancel`: cancels an income or expense by registering its reversal. */
export function cancelOperation(operationId: string): Promise<void> {
  return request<void>('POST', `/operations/${encodeURIComponent(operationId)}/cancel`);
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
