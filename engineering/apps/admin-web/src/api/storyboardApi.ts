import request from './client'
import type {
  ApiResponse,
  ActionStatusResponse,
  StoryboardDraftSummaryResponse,
  StoryboardDraftResponse,
  StoryboardDraftEditPayload,
  StoryboardGenerateResponse,
  StoryboardRejectRequest,
} from '../types'

/**
 * FP-003: Storyboard Workflow API çağrıları.
 * Generic status update yok — sadece action endpoint'ler.
 * Internal AI contract DTO'ları expose edilmez.
 * mediaGenerationNotes gönderilmez.
 */

export function generateStoryboardForScenario(
  scenarioId: string
): Promise<ApiResponse<StoryboardGenerateResponse>> {
  return request<StoryboardGenerateResponse>(`/admin/scenarios/${scenarioId}/storyboards/generate`, {
    method: 'POST',
  })
}

export function listStoryboardsByContentProject(
  projectId: string
): Promise<ApiResponse<StoryboardDraftSummaryResponse[]>> {
  return request<StoryboardDraftSummaryResponse[]>(`/admin/content-projects/${projectId}/storyboards`)
}

export function getStoryboardDetail(
  storyboardId: string
): Promise<ApiResponse<StoryboardDraftResponse>> {
  return request<StoryboardDraftResponse>(`/admin/storyboards/${storyboardId}`)
}

export function updateStoryboardDraft(
  storyboardId: string,
  payload: StoryboardDraftEditPayload
): Promise<ApiResponse<StoryboardDraftResponse>> {
  return request<StoryboardDraftResponse>(`/admin/storyboards/${storyboardId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function approveStoryboardDraft(
  storyboardId: string
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/storyboards/${storyboardId}/approve`, {
    method: 'POST',
  })
}

export function rejectStoryboardDraft(
  storyboardId: string,
  data: StoryboardRejectRequest
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/storyboards/${storyboardId}/reject`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}
