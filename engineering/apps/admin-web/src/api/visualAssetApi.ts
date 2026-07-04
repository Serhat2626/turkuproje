import request from './client'
import type {
  ApiResponse,
  ActionStatusResponse,
  VisualAssetPlanResponse,
  VisualAssetPlanSummaryResponse,
  VisualAssetPlanGenerateResponse,
  VisualAssetPlanEditRequest,
  SceneVisualPlanEditRequest,
  ThumbnailPlanEditRequest,
  VisualAssetPlanRejectRequest,
} from '../types'

/**
 * FP-006: Visual Asset Planning Workflow API çağrıları.
 * Generic status update yok — sadece action endpoint'ler.
 * Internal AI contract DTO'ları expose edilmez.
 * imageUrl, mediaAssetId, generatedAssetId alanları dahil edilmez.
 */

export function generateVisualAssetPlan(
  storyboardDraftId: string
): Promise<ApiResponse<VisualAssetPlanGenerateResponse>> {
  return request<VisualAssetPlanGenerateResponse>(`/admin/storyboard-drafts/${storyboardDraftId}/visual-asset-plans/generate`, {
    method: 'POST',
  })
}

export function listVisualAssetPlansByStoryboard(
  storyboardDraftId: string
): Promise<ApiResponse<VisualAssetPlanSummaryResponse[]>> {
  return request<VisualAssetPlanSummaryResponse[]>(`/admin/storyboard-drafts/${storyboardDraftId}/visual-asset-plans`)
}

export function listVisualAssetPlansByContentProject(
  contentProjectId: string
): Promise<ApiResponse<VisualAssetPlanSummaryResponse[]>> {
  return request<VisualAssetPlanSummaryResponse[]>(`/admin/content-projects/${contentProjectId}/visual-asset-plans`)
}

export function getVisualAssetPlanDetail(
  planId: string
): Promise<ApiResponse<VisualAssetPlanResponse>> {
  return request<VisualAssetPlanResponse>(`/admin/visual-asset-plans/${planId}`)
}

export function updateVisualAssetPlan(
  planId: string,
  payload: VisualAssetPlanEditRequest
): Promise<ApiResponse<VisualAssetPlanResponse>> {
  return request<VisualAssetPlanResponse>(`/admin/visual-asset-plans/${planId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function updateSceneVisualPlan(
  planId: string,
  sceneNumber: number,
  payload: SceneVisualPlanEditRequest
): Promise<ApiResponse<VisualAssetPlanResponse>> {
  return request<VisualAssetPlanResponse>(`/admin/visual-asset-plans/${planId}/scenes/${sceneNumber}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function updateThumbnailPlan(
  planId: string,
  payload: ThumbnailPlanEditRequest
): Promise<ApiResponse<VisualAssetPlanResponse>> {
  return request<VisualAssetPlanResponse>(`/admin/visual-asset-plans/${planId}/thumbnail`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function approveVisualAssetPlan(
  planId: string
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/visual-asset-plans/${planId}/approve`, {
    method: 'POST',
  })
}

export function rejectVisualAssetPlan(
  planId: string,
  data: VisualAssetPlanRejectRequest
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/visual-asset-plans/${planId}/reject`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}
