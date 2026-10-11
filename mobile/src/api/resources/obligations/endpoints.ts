import { request } from '../../client';
import type { IsoDate } from '../common';

import type {
  CreateObligationRequest,
  CreateObligationResponse,
  ModifyObligationRequest,
  ObligationOccurrencesResponse,
  ObligationResponse,
  PayOccurrenceRequest,
  PayOccurrenceResponse,
  SkipOverdueOccurrencesResponse,
} from './types';

// Endpoints of the obligations resource: `/api/v1/obligations` and their occurrences
// (`entry.web.obligation`).

/** Path of one obligation. */
function obligationPath(obligationId: string): string {
  return `/obligations/${encodeURIComponent(obligationId)}`;
}

/** Path of one occurrence of an obligation. */
function occurrencePath(obligationId: string, dueDate: IsoDate): string {
  return `${obligationPath(obligationId)}/occurrences/${encodeURIComponent(dueDate)}`;
}

/** `GET /obligations`: every obligation of the user, active and archived, ordered by name then id. */
export function listObligations(): Promise<ObligationResponse[]> {
  return request<ObligationResponse[]>('GET', '/obligations');
}

/** `GET /obligations/{id}`: one obligation; 404 when it does not exist. */
export function getObligation(obligationId: string): Promise<ObligationResponse> {
  return request<ObligationResponse>('GET', obligationPath(obligationId));
}

/** `GET /obligations/{id}/occurrences`: what is overdue, coming up, and recently resolved. */
export function listObligationOccurrences(obligationId: string): Promise<ObligationOccurrencesResponse> {
  return request<ObligationOccurrencesResponse>('GET', `${obligationPath(obligationId)}/occurrences`);
}

/** `POST /obligations`: creates an obligation. Names are unique per user, ignoring case. */
export function createObligation(body: CreateObligationRequest): Promise<CreateObligationResponse> {
  return request<CreateObligationResponse>('POST', '/obligations', body);
}

/** `PATCH /obligations/{id}`: changes only the fields in the body (at least one). */
export function modifyObligation(obligationId: string, body: ModifyObligationRequest): Promise<void> {
  return request<void>('PATCH', obligationPath(obligationId), body);
}

/** `POST /obligations/{id}/archive`: archives an obligation. There is no way back. */
export function archiveObligation(obligationId: string): Promise<void> {
  return request<void>('POST', `${obligationPath(obligationId)}/archive`);
}

/**
 * `POST /obligations/{id}/occurrences/{dueDate}/pay`: pays the occurrence, with the obligation's
 * amount, account, and category, dated today, unless `body` changes them for this payment. Not
 * idempotent beyond the server's `OCCURRENCE_ALREADY_RESOLVED` guard.
 */
export function payOccurrence(
  obligationId: string,
  dueDate: IsoDate,
  body?: PayOccurrenceRequest,
): Promise<PayOccurrenceResponse> {
  return request<PayOccurrenceResponse>('POST', `${occurrencePath(obligationId, dueDate)}/pay`, body);
}

/** `POST /obligations/{id}/occurrences/{dueDate}/skip`: skips an occurrence without an expense. */
export function skipOccurrence(obligationId: string, dueDate: IsoDate): Promise<void> {
  return request<void>('POST', `${occurrencePath(obligationId, dueDate)}/skip`);
}

/**
 * `POST /obligations/{id}/occurrences/{dueDate}/reopen`: reopens a skipped occurrence. A paid one
 * answers 409 `OCCURRENCE_PAID_NOT_REOPENABLE`: it is reopened by cancelling its expense.
 */
export function reopenOccurrence(obligationId: string, dueDate: IsoDate): Promise<void> {
  return request<void>('POST', `${occurrencePath(obligationId, dueDate)}/reopen`);
}

/** `POST /obligations/{id}/overdue-occurrences/skip`: skips every overdue occurrence at once. */
export function skipOverdueOccurrences(obligationId: string): Promise<SkipOverdueOccurrencesResponse> {
  return request<SkipOverdueOccurrencesResponse>('POST', `${obligationPath(obligationId)}/overdue-occurrences/skip`);
}
