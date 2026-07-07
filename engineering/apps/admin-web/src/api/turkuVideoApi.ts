import request from './client'
import type {
  ApiResponse,
  TurkuVideoBootstrapRequest,
  TurkuVideoBootstrapResponse,
  TurkuVideoProjectResponse,
  TurkuVideoProgressResponse,
  TurkuPreparationResponse,
  TurkuAssetPreparationResponse,
  TurkuImageBatchResponse,
  TurkuGenerationSafetyResponse,
  TurkuProviderPlanningGenerationResponse,
  TurkuPlanningBundleGenerationResponse,
  TurkuImageBatchRunResponse,
} from '../types'

/**
 * FP-019 Task 05: Türkü Video Project admin API client.
 *
 * Safe internal workflow endpoints only.
 * actorRole is TEST_FALLBACK compatibility only.
 */

const BASE = '/admin/turku-video-projects'

export function bootstrapTurkuVideoProject(
  data: TurkuVideoBootstrapRequest
): Promise<ApiResponse<TurkuVideoBootstrapResponse>> {
  return request<TurkuVideoBootstrapResponse>(`${BASE}/bootstrap`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function getTurkuVideoProject(
  id: string
): Promise<ApiResponse<TurkuVideoProjectResponse>> {
  return request<TurkuVideoProjectResponse>(`${BASE}/${id}`)
}

export function getTurkuVideoProjectProgress(
  id: string
): Promise<ApiResponse<TurkuVideoProgressResponse>> {
  return request<TurkuVideoProgressResponse>(`${BASE}/${id}/progress`)
}

export function prepareTurkuStory(
  id: string
): Promise<ApiResponse<TurkuPreparationResponse>> {
  return request<TurkuPreparationResponse>(`${BASE}/${id}/prepare-story`, {
    method: 'POST',
  })
}

export function prepareTurkuScenario(
  id: string
): Promise<ApiResponse<TurkuPreparationResponse>> {
  return request<TurkuPreparationResponse>(`${BASE}/${id}/prepare-scenario`, {
    method: 'POST',
  })
}

export function prepareTurkuStoryboard(
  id: string
): Promise<ApiResponse<TurkuPreparationResponse>> {
  return request<TurkuPreparationResponse>(`${BASE}/${id}/prepare-storyboard`, {
    method: 'POST',
  })
}

export function prepareTurkuSceneAssets(
  id: string
): Promise<ApiResponse<TurkuAssetPreparationResponse>> {
  return request<TurkuAssetPreparationResponse>(`${BASE}/${id}/prepare-scene-assets`, {
    method: 'POST',
  })
}

export function prepareTurkuImageBatch(
  id: string
): Promise<ApiResponse<TurkuImageBatchResponse>> {
  return request<TurkuImageBatchResponse>(`${BASE}/${id}/prepare-image-batch`, {
    method: 'POST',
  })
}

export function completeTurkuImageBatch(
  id: string,
  partial = false
): Promise<ApiResponse<TurkuImageBatchResponse>> {
  return request<TurkuImageBatchResponse>(`${BASE}/${id}/complete-image-batch`, {
    method: 'POST',
    body: JSON.stringify({ partial }),
  })
}

// ── FP-020: Provider-Draft Generation & Safety ──

export function generateTurkuStory(
  id: string
): Promise<ApiResponse<TurkuProviderPlanningGenerationResponse>> {
  return request<TurkuProviderPlanningGenerationResponse>(`${BASE}/${id}/generate-story`, {
    method: 'POST',
  })
}

export function generateTurkuScenario(
  id: string
): Promise<ApiResponse<TurkuProviderPlanningGenerationResponse>> {
  return request<TurkuProviderPlanningGenerationResponse>(`${BASE}/${id}/generate-scenario`, {
    method: 'POST',
  })
}

export function generateTurkuStoryboard(
  id: string
): Promise<ApiResponse<TurkuProviderPlanningGenerationResponse>> {
  return request<TurkuProviderPlanningGenerationResponse>(`${BASE}/${id}/generate-storyboard`, {
    method: 'POST',
  })
}

export function generateTurkuPlanningBundle(
  id: string
): Promise<ApiResponse<TurkuPlanningBundleGenerationResponse>> {
  return request<TurkuPlanningBundleGenerationResponse>(`${BASE}/${id}/generate-planning-bundle`, {
    method: 'POST',
  })
}

export function runTurkuImageBatch(
  id: string
): Promise<ApiResponse<TurkuImageBatchRunResponse>> {
  return request<TurkuImageBatchRunResponse>(`${BASE}/${id}/run-image-batch`, {
    method: 'POST',
  })
}

export function getTurkuGenerationSafety(
  id: string
): Promise<ApiResponse<TurkuGenerationSafetyResponse>> {
  return request<TurkuGenerationSafetyResponse>(`${BASE}/${id}/generation-safety`)
}
