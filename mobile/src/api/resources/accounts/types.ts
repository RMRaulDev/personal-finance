// Types of the accounts resource: `/api/v1/accounts` (`entry.web.account`).

/** Status of an account. Only active accounts take new operations. */
export type AccountStatus = 'ACTIVE' | 'INACTIVE';

/** One account, from `GET /accounts`. `openingBalanceCents` is the balance it started with. */
export interface AccountResponse {
  id: string;
  name: string;
  balanceCents: number;
  openingBalanceCents: number;
  status: AccountStatus;
}
