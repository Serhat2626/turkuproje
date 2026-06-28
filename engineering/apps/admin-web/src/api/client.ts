import type { ApiResponse } from '../types';

const API_BASE = '/api';

/**
 * Ortak fetch wrapper — tüm API çağrıları buradan geçer.
 */
async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const response = await fetch(`${API_BASE}${endpoint}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  });

  const body: ApiResponse<T> = await response.json();

  if (!response.ok) {
    throw body;
  }

  return body;
}

export default request;
