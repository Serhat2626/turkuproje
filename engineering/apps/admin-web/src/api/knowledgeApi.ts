import request from './client';
import type {
  ApiResponse,
  ActionStatusResponse,
  CreateKnowledgeItemRequest,
  KnowledgeItemResponse,
  KnowledgeItemSummaryResponse,
  UpdateKnowledgeItemRequest,
} from '../types';

/**
 * FP-001: Knowledge Item API çağrıları.
 * Endpoint: /api/admin/knowledge-items
 * Generic status update yok — sadece action endpoint'ler.
 */

export function listKnowledgeItems(
  status?: string
): Promise<ApiResponse<KnowledgeItemSummaryResponse[]>> {
  const params = status ? `?status=${status}` : '';
  return request<KnowledgeItemSummaryResponse[]>(`/admin/knowledge-items${params}`);
}

export function getKnowledgeItem(
  id: string
): Promise<ApiResponse<KnowledgeItemResponse>> {
  return request<KnowledgeItemResponse>(`/admin/knowledge-items/${id}`);
}

export function createKnowledgeItem(
  data: CreateKnowledgeItemRequest
): Promise<ApiResponse<KnowledgeItemResponse>> {
  return request<KnowledgeItemResponse>('/admin/knowledge-items', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateKnowledgeItem(
  id: string,
  data: UpdateKnowledgeItemRequest
): Promise<ApiResponse<KnowledgeItemResponse>> {
  return request<KnowledgeItemResponse>(`/admin/knowledge-items/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function markKnowledgeItemReady(
  id: string
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/knowledge-items/${id}/mark-ready`, {
    method: 'POST',
  });
}

export function archiveKnowledgeItem(
  id: string
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/knowledge-items/${id}/archive`, {
    method: 'POST',
  });
}
