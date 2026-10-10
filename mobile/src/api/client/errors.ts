/** Stable codes of a 409 response (`BusinessRuleCode` in the backend). */
export type BusinessRuleCode =
  | 'INSUFFICIENT_BALANCE'
  | 'ACCOUNT_INACTIVE'
  | 'CATEGORY_INACTIVE'
  | 'OPERATION_ALREADY_CANCELLED'
  | 'ACCOUNT_NAME_ALREADY_EXISTS'
  | 'CATEGORY_NAME_ALREADY_EXISTS'
  | 'OBLIGATION_NAME_ALREADY_EXISTS'
  | 'OBLIGATION_ARCHIVED'
  | 'OBLIGATION_HAS_OVERDUE_OCCURRENCES'
  | 'OCCURRENCE_ALREADY_RESOLVED'
  | 'OCCURRENCE_NOT_SCHEDULED'
  | 'OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION'
  | 'OCCURRENCE_PAID_NOT_REOPENABLE'
  | 'INITIAL_SETUP_ALREADY_COMPLETED';

/** Why a request failed before reaching the server or getting a usable answer. */
export type ApiFailureKind = 'http' | 'network' | 'not-configured';

/**
 * A failed API call. HTTP errors carry the RFC 9457 problem details; `status` is 0 when the server
 * was never reached. UI copy must be chosen from `kind`, `status`, and `code`, never from `detail`.
 */
export class ApiError extends Error {
  /** Where the failure happened. */
  readonly kind: ApiFailureKind;
  /** HTTP status, or 0 when there was no HTTP response. */
  readonly status: number;
  /** Problem `title`, when the server sent one. */
  readonly title: string | undefined;
  /** Problem `detail`, for logs only. */
  readonly detail: string | undefined;
  /** Stable business rule code of a 409. */
  readonly code: string | undefined;

  /** Builds an error from its parts; `message` is meant for logs. */
  constructor(params: {
    kind: ApiFailureKind;
    status: number;
    title?: string;
    detail?: string;
    code?: string;
    message: string;
  }) {
    super(params.message);
    this.name = 'ApiError';
    this.kind = params.kind;
    this.status = params.status;
    this.title = params.title;
    this.detail = params.detail;
    this.code = params.code;
  }
}

/** Narrows an unknown thrown value to an `ApiError`. */
export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}
