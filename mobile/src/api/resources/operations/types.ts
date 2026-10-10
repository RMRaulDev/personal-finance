import type { IsoDate } from '../common';

// Types of the operations resource: `/api/v1/operations` (`entry.web.operation`).

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
