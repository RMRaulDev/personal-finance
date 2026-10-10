import { queryOptions } from '@tanstack/react-query';

import { request } from './client';
import { queryKeys } from './queryClient';

// The categories resource: `/api/v1/categories` (`entry.web.category`).

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

/** `GET /categories`: every category of the user, both types and all statuses, ordered by name then id. */
export function listCategories(): Promise<CategoryResponse[]> {
  return request<CategoryResponse[]>('GET', '/categories');
}

/** Query options of the category list, for `useQuery(categoriesQuery())`. */
export function categoriesQuery() {
  return queryOptions({ queryKey: queryKeys.categories, queryFn: listCategories });
}
