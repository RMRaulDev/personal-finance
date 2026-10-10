import { request } from '../../client';
import type { IsoDate } from '../common';

import type { ObligationResponse, PayOccurrenceResponse } from './types';

// Endpoints of the obligations resource: `/api/v1/obligations` and their occurrences
// (`entry.web.obligation`).

/** `GET /obligations`: every obligation of the user, active and archived. */
export function listObligations(): Promise<ObligationResponse[]> {
  return request<ObligationResponse[]>('GET', '/obligations');
}

/**
 * `POST /obligations/{id}/occurrences/{dueDate}/pay` without a body: pays the occurrence with the
 * obligation's amount, account, and category, dated today. Not idempotent beyond the server's
 * `OCCURRENCE_ALREADY_RESOLVED` guard.
 */
export function payOccurrence(obligationId: string, dueDate: IsoDate): Promise<PayOccurrenceResponse> {
  return request<PayOccurrenceResponse>(
    'POST',
    `/obligations/${encodeURIComponent(obligationId)}/occurrences/${encodeURIComponent(dueDate)}/pay`,
  );
}
