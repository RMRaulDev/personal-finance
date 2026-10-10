export { accountsQuery, listAccounts, type AccountResponse, type AccountStatus } from './resources/accounts';
export {
  categoriesQuery,
  listCategories,
  type CategoryResponse,
  type CategoryStatus,
  type CategoryType,
} from './resources/categories';
export type * from './resources/common';
export {
  dashboardQuery,
  getDashboard,
  type AccountShortfallItem,
  type AttentionItem,
  type AvailableToSpend,
  type DashboardResponse,
  type Horizon,
  type OverdueOccurrenceItem,
  type PaymentBlockedItem,
  type RecentActivityItem,
  type ShortfallItem,
  type UpcomingCommitment,
} from './resources/dashboard';
export { ApiError, isApiError, queryClient, queryKeys, type ApiFailureKind, type BusinessRuleCode } from './client';
export {
  listObligations,
  obligationsQuery,
  payOccurrence,
  usePayOccurrence,
  type Frequency,
  type ObligationResponse,
  type PayOccurrenceResponse,
  type PayOccurrenceVariables,
  type Recurrence,
} from './resources/obligations';
export {
  cancelOperation,
  registerExpense,
  registerIncome,
  useCancelOperation,
  useRegisterExpense,
  useRegisterIncome,
  type RegisterOperationRequest,
  type RegisterOperationResponse,
} from './resources/operations';
export {
  completeInitialSetup,
  useCompleteInitialSetup,
  type CompleteInitialSetupRequest,
  type CompleteInitialSetupResponse,
} from './resources/setup';
