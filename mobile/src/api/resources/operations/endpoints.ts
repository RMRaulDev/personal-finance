import { request } from '../../client';

import type { RegisterOperationRequest, RegisterOperationResponse } from './types';

// Endpoints of the operations resource: `/api/v1/operations` (`entry.web.operation`).

/** `POST /operations/expenses`: registers an expense. Not idempotent: a retry registers it twice. */
export function registerExpense(body: RegisterOperationRequest): Promise<RegisterOperationResponse> {
  return request<RegisterOperationResponse>('POST', '/operations/expenses', body);
}

/** `POST /operations/incomes`: registers an income. Not idempotent: a retry registers it twice. */
export function registerIncome(body: RegisterOperationRequest): Promise<RegisterOperationResponse> {
  return request<RegisterOperationResponse>('POST', '/operations/incomes', body);
}

/** `POST /operations/{id}/cancel`: cancels an income or expense by registering its reversal. */
export function cancelOperation(operationId: string): Promise<void> {
  return request<void>('POST', `/operations/${encodeURIComponent(operationId)}/cancel`);
}
