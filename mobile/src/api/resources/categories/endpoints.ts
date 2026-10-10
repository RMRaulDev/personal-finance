import { request } from '../../client';

import type { CategoryResponse } from './types';

// Endpoints of the categories resource: `/api/v1/categories` (`entry.web.category`).

/** `GET /categories`: every category of the user, both types and all statuses, ordered by name then id. */
export function listCategories(): Promise<CategoryResponse[]> {
  return request<CategoryResponse[]>('GET', '/categories');
}
