export { accountsQuery, listAccounts, type AccountResponse, type AccountStatus } from './accounts';
export {
  categoriesQuery,
  listCategories,
  type CategoryResponse,
  type CategoryStatus,
  type CategoryType,
} from './categories';
export type * from './resources/common/types';
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
} from './resources/dashboard/types';
export { ApiError, isApiError, type ApiFailureKind, type BusinessRuleCode } from './client/errors';
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
} from './obligations';
export {
  cancelOperation,
  registerExpense,
  registerIncome,
  useCancelOperation,
  useRegisterExpense,
  useRegisterIncome,
  type RegisterOperationRequest,
  type RegisterOperationResponse,
} from './operations';
export { queryClient, queryKeys } from './client/queryClient';
export {
  completeInitialSetup,
  useCompleteInitialSetup,
  type CompleteInitialSetupRequest,
  type CompleteInitialSetupResponse,
} from './resources/setup/mutations';
