import { queryOptions } from '@tanstack/react-query';

import { queryKeys } from '../../client';

import { listCategories } from './endpoints';

/** Query options of the category list, for `useQuery(categoriesQuery())`. */
export function categoriesQuery() {
  return queryOptions({ queryKey: queryKeys.categories, queryFn: listCategories });
}
