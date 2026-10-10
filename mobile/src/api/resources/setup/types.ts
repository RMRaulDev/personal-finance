// Types of the initial setup: `POST /api/v1/setup` (`entry.web.setup`).

/** Body of `POST /setup`: the first account and its opening balance (0 or more). */
export interface CompleteInitialSetupRequest {
  accountName: string;
  openingBalanceCents: number;
}

/** Response of `POST /setup`: the id of the account it created. */
export interface CompleteInitialSetupResponse {
  id: string;
}
