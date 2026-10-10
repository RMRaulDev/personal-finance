// Types of the categories resource: `/api/v1/categories` (`entry.web.category`).

/** Whether a category classifies incomes or expenses. */
export type CategoryType = 'INCOME' | 'EXPENSE';

/** Status of a category. Only active categories take new operations. */
export type CategoryStatus = 'ACTIVE' | 'INACTIVE';

/** One category, from `GET /categories`. */
export interface CategoryResponse {
  id: string;
  name: string;
  type: CategoryType;
  status: CategoryStatus;
}
