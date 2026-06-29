import request from './client';
import type {
  ApiResponse,
  ActionStatusResponse,
  AiGenerationSummaryResponse,
  ApproveRequest,
  ContentProjectResponse,
  ContentProjectSummaryResponse,
  CreateContentProjectRequest,
  GenerateAiDraftRequest,
  ReviewNoteRequest,
} from '../types';

/**
 * FP-001: Content Project API çağrıları.
 * Endpoint: /api/admin/content-projects
 * Generic status update yok — sadece action endpoint'ler.
 */

export function listContentProjects(
  status?: string,
  knowledgeItemId?: string
): Promise<ApiResponse<ContentProjectSummaryResponse[]>> {
  const params = new URLSearchParams();
  if (status) params.set('status', status);
  if (knowledgeItemId) params.set('knowledgeItemId', knowledgeItemId);
  const qs = params.toString();
  return request<ContentProjectSummaryResponse[]>(`/admin/content-projects${qs ? `?${qs}` : ''}`);
}

export function getContentProject(
  id: string
): Promise<ApiResponse<ContentProjectResponse>> {
  return request<ContentProjectResponse>(`/admin/content-projects/${id}`);
}

export function createContentProject(
  data: CreateContentProjectRequest
): Promise<ApiResponse<ContentProjectResponse>> {
  return request<ContentProjectResponse>('/admin/content-projects', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function generateAiDraft(
  id: string,
  data?: GenerateAiDraftRequest
): Promise<ApiResponse<AiGenerationSummaryResponse>> {
  return request<AiGenerationSummaryResponse>(`/admin/content-projects/${id}/generate-ai-draft`, {
    method: 'POST',
    body: JSON.stringify(data ?? {}),
  });
}

export function retryAiDraft(
  id: string
): Promise<ApiResponse<AiGenerationSummaryResponse>> {
  return request<AiGenerationSummaryResponse>(`/admin/content-projects/${id}/retry-ai-draft`, {
    method: 'POST',
  });
}

export function startContentProjectReview(
  id: string
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/content-projects/${id}/start-review`, {
    method: 'POST',
  });
}

export function approveContentProject(
  id: string,
  data?: ApproveRequest
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/content-projects/${id}/approve`, {
    method: 'POST',
    body: JSON.stringify(data ?? {}),
  });
}

export function requestContentProjectRevision(
  id: string,
  data: ReviewNoteRequest
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/content-projects/${id}/request-revision`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function rejectContentProject(
  id: string,
  data: ReviewNoteRequest
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/content-projects/${id}/reject`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function markContentProjectReadyForPublish(
  id: string
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/content-projects/${id}/mark-ready-for-publish`, {
    method: 'POST',
  });
}

export function cancelContentProject(
  id: string
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/content-projects/${id}/cancel`, {
    method: 'POST',
  });
}
