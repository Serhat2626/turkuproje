import request from './client';
import type {
  ApiResponse,
  CreateKnowledgeItemRequest,
  KnowledgeItemResponse,
} from '../types';

/**
 * Bilgi öğesi API çağrıları.
 */
export function createKnowledgeItem(
  data: CreateKnowledgeItemRequest
): Promise<ApiResponse<KnowledgeItemResponse>> {
  return request<KnowledgeItemResponse>('/knowledge-items', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}
