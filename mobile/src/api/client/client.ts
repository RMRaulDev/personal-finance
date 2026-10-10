import { ApiError } from './errors';

/** HTTP methods the API uses. */
type HttpMethod = 'GET' | 'POST';

const API_PREFIX = '/api/v1';

// React Native's fetch has no timeout of its own and can wait a long time for an unreachable LAN
// address, which would leave the screen loading or a payment pending indefinitely.
const REQUEST_TIMEOUT_MS = 15_000;

/** Problem details fields the client reads from an error body. */
interface ProblemBody {
  title?: unknown;
  detail?: unknown;
  code?: unknown;
}

/** Returns the value when it is a string, or undefined otherwise. */
function stringOrUndefined(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined;
}

/**
 * Base URL of the backend, from `EXPO_PUBLIC_API_URL` (set in mobile/.env.local). Expo inlines
 * `EXPO_PUBLIC_*` variables at build time, so it must be read with this exact expression.
 */
function apiBaseUrl(): string | undefined {
  const url = process.env.EXPO_PUBLIC_API_URL;
  return url ? url.replace(/\/+$/, '') : undefined;
}

/** Reads a problem+json body; tolerates an empty or non-JSON body. */
async function readProblem(response: Response): Promise<ProblemBody> {
  try {
    const body: unknown = await response.json();
    return typeof body === 'object' && body !== null ? body : {};
  } catch {
    return {};
  }
}

/**
 * Sends a request to the API and returns its JSON body (`undefined` for 204). Throws `ApiError`
 * for a missing base URL, a network failure, or any non-2xx answer.
 */
export async function request<T>(method: HttpMethod, path: string, body?: unknown): Promise<T> {
  const baseUrl = apiBaseUrl();
  if (!baseUrl) {
    throw new ApiError({ kind: 'not-configured', status: 0, message: 'EXPO_PUBLIC_API_URL is not set' });
  }

  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${API_PREFIX}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
    });
  } catch (cause) {
    // A timeout aborts the request; to the user it is the same as no connection.
    throw new ApiError({ kind: 'network', status: 0, message: `Network request failed: ${String(cause)}` });
  } finally {
    clearTimeout(timeout);
  }

  if (!response.ok) {
    const problem = await readProblem(response);
    throw new ApiError({
      kind: 'http',
      status: response.status,
      title: stringOrUndefined(problem.title),
      detail: stringOrUndefined(problem.detail),
      code: stringOrUndefined(problem.code),
      message: `${method} ${path} answered ${response.status}`,
    });
  }

  if (response.status === 204) {
    // Actions answer 204 without a body; their callers expect `void`. The cast is safe because
    // only requests typed as `void` get a 204, so no caller reads a value that is not there.
    return undefined as T;
  }
  // The body is trusted to match T: there is no runtime validation by design (mobile.md → API client).
  // The cast is safe because T is the type the endpoint documents; nothing here can check it.
  return (await response.json()) as T;
}
