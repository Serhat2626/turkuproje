import request from './client';
import type {
  ApiResponse,
  ContentDraftResponse,
} from '../types';

/**
 * FP-001: Content Draft API çağrıları.
 * Endpoint: /api/admin/content-drafts
 * Sadece GET (detay) ve PUT (düzenleme). Status alanı gönderilmez.
 */

export function getContentDraft(
  id: string
): Promise<ApiResponse<ContentDraftResponse>> {
  return request<ContentDraftResponse>(`/admin/content-drafts/${id}`);
}

export function updateContentDraft(
  id: string,
  data: { title?: string; body?: string; summary?: string }
): Promise<ApiResponse<ContentDraftResponse>> {
  return request<ContentDraftResponse>(`/admin/content-drafts/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}
