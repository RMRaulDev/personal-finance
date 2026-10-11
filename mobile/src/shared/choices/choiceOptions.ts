import type { AccountResponse, CategoryResponse, CategoryType } from '../../api';
import type { ChoiceOption } from '../../design-system';

// Accounts and categories as the choices of a form (the registration sheet, the obligation form,
// the payment sheet), so every screen offers them the same way.

/** Accounts that take new operations, in the API's order. */
export function activeAccounts(accounts: AccountResponse[]): AccountResponse[] {
  return accounts.filter((account) => account.status === 'ACTIVE');
}

/** Chips of an account choice, for a user with several active accounts. */
export function accountOptions(accounts: AccountResponse[]): ChoiceOption<string>[] {
  return accounts.map((account) => ({ value: account.id, label: account.name }));
}

/** Chips of a category choice: the active categories of one type, in the API's order. */
export function categoryOptions(categories: CategoryResponse[], type: CategoryType): ChoiceOption<string>[] {
  return categories
    .filter((category) => category.type === type && category.status === 'ACTIVE')
    .map((category) => ({ value: category.id, label: category.name }));
}
