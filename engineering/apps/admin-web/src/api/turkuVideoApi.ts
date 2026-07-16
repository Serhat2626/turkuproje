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
  TurkuCompositionPlanResponse,
  TurkuRenderManifestResponse,
  TurkuRenderReadinessReport,
  TurkuCompositionPreviewResponse,
  TurkuRenderJobResponse,
  TurkuRenderExecutionResponse,
  TurkuRenderExecutionSafetyResponse,
  TurkuInternalDryRunTriggerRequest,
  TurkuInternalDryRunTriggerResponse,
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

// ── FP-021: Composition / Render Planning ──

export function prepareTurkuCompositionPlan(
  id: string
): Promise<ApiResponse<TurkuCompositionPlanResponse>> {
  return request<TurkuCompositionPlanResponse>(`${BASE}/${id}/prepare-composition-plan`, {
    method: 'POST',
  })
}

export function prepareTurkuRenderManifest(
  id: string
): Promise<ApiResponse<TurkuRenderManifestResponse>> {
  return request<TurkuRenderManifestResponse>(`${BASE}/${id}/prepare-render-manifest`, {
    method: 'POST',
  })
}

export function getTurkuRenderReadiness(
  id: string
): Promise<ApiResponse<TurkuRenderReadinessReport>> {
  return request<TurkuRenderReadinessReport>(`${BASE}/${id}/render-readiness`)
}

export function getTurkuCompositionPreview(
  id: string
): Promise<ApiResponse<TurkuCompositionPreviewResponse>> {
  return request<TurkuCompositionPreviewResponse>(`${BASE}/${id}/composition-preview`)
}

// ── FP-022: Render Job + Execution Safety ──

export function prepareTurkuRenderJob(
  id: string
): Promise<ApiResponse<TurkuRenderJobResponse>> {
  return request<TurkuRenderJobResponse>(`${BASE}/${id}/prepare-render-job`, {
    method: 'POST',
  })
}

export function getTurkuRenderJob(
  id: string
): Promise<ApiResponse<TurkuRenderJobResponse>> {
  return request<TurkuRenderJobResponse>(`${BASE}/${id}/render-job`)
}

export function runTurkuRenderJob(
  id: string
): Promise<ApiResponse<TurkuRenderExecutionResponse>> {
  return request<TurkuRenderExecutionResponse>(`${BASE}/${id}/run-render-job`, {
    method: 'POST',
  })
}

export function getTurkuRenderExecutionSafety(
  id: string
): Promise<ApiResponse<TurkuRenderExecutionSafetyResponse>> {
  return request<TurkuRenderExecutionSafetyResponse>(`${BASE}/${id}/render-execution-safety`)
}

// FP-049: Internal dry-run trigger

export function triggerInternalDryRun(
  id: string,
  data: TurkuInternalDryRunTriggerRequest
): Promise<ApiResponse<TurkuInternalDryRunTriggerResponse>> {
  return request<TurkuInternalDryRunTriggerResponse>(`${BASE}/${id}/trigger-internal-dry-run`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function getInternalDryRunStatus(
  id: string
): Promise<ApiResponse<TurkuInternalDryRunTriggerResponse>> {
  return request<TurkuInternalDryRunTriggerResponse>(`${BASE}/${id}/internal-dry-run-status`)
}

// ── Content fetch ──

export function getTurkuVideoProjectContent(
  id: string
): Promise<ApiResponse<any>> {
  return request<any>(`${BASE}/${id}/content`)
}

// ── TTS generation ──

export function generateTts(
  id: string
): Promise<ApiResponse<any>> {
  return request<any>(`${BASE}/${id}/generate-tts`, { method: 'POST' })
}

// ── Media URLs ──

export function getSceneImageUrl(id: string, sceneNo: number): string {
  return `/api/admin/turku-video-projects/${id}/scenes/${sceneNo}/image`
}

export function getSceneAudioUrl(id: string, sceneNo: number): string {
  return `/api/admin/turku-video-projects/${id}/scenes/${sceneNo}/audio`
}

// ── Video Assembly ──

export function assembleVideo(
  id: string
): Promise<ApiResponse<any>> {
  return request<any>(`${BASE}/${id}/assemble-video`, { method: 'POST' })
}

export function getVideoUrl(id: string): string {
  return `/api/admin/turku-video-projects/${id}/video`
}

// ── Full Pipeline ──

export function generateFullVideo(
  id: string,
  options?: { includeKling?: boolean }
): Promise<ApiResponse<any>> {
  const params = new URLSearchParams()
  // Backend varsayılan: includeKling=true (paralel Kling ~5-7dk)
  // Sadece kapatmak istendiğinde gönder
  if (options?.includeKling === false) params.set('includeKling', 'false')
  const qs = params.toString() ? `?${params.toString()}` : ''
  // Paralel Kling: ~20dk max (görseller + Kling + TTS + FFmpeg)
  return request<any>(`${BASE}/${id}/generate-full-video${qs}`, {
    method: 'POST',
    timeoutMs: 20 * 60 * 1000,
  })
}

export function generateAllImages(
  id: string
): Promise<ApiResponse<any>> {
  // 35 sahne görsel üretimi Fal.ai ile uzun sürebilir
  return request<any>(`${BASE}/${id}/generate-images`, {
    method: 'POST',
    timeoutMs: 10 * 60 * 1000,
  })
}

// ── Music Upload ──

export async function uploadMusic(
  id: string,
  type: 'fon' | 'turku',
  file: File
): Promise<ApiResponse<any>> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)

  const token = localStorage.getItem('aa_auth_token')
  const res = await fetch(`/api${BASE}/${id}/upload-music`, {
    method: 'POST',
    headers: token ? { 'Authorization': `Bearer ${token}` } : {},
    body: formData,
  })
  return res.json()
}

export function getMusicStatus(
  id: string
): Promise<ApiResponse<{ fonUploaded: boolean; turkuUploaded: boolean }>> {
  return request<{ fonUploaded: boolean; turkuUploaded: boolean }>(`${BASE}/${id}/music-status`)
}
