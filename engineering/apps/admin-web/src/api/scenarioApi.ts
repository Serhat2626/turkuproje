import request from './client'
import type {
  ApiResponse,
  ActionStatusResponse,
  ScenarioDraftSummaryResponse,
  ScenarioDraftResponse,
  ScenarioDraftEditPayload,
  ScenarioGenerateResponse,
  ScenarioRejectRequest,
} from '../types'

/**
 * FP-002: Scenario Workflow API çağrıları.
 * Generic status update yok — sadece action endpoint'ler.
 * Internal AI contract DTO'ları expose edilmez.
 */

export function generateScenario(
  projectId: string
): Promise<ApiResponse<ScenarioGenerateResponse>> {
  return request<ScenarioGenerateResponse>(`/admin/content-projects/${projectId}/scenarios/generate`, {
    method: 'POST',
  })
}

export function listProjectScenarios(
  projectId: string
): Promise<ApiResponse<ScenarioDraftSummaryResponse[]>> {
  return request<ScenarioDraftSummaryResponse[]>(`/admin/content-projects/${projectId}/scenarios`)
}

export function getScenarioDetail(
  scenarioId: string
): Promise<ApiResponse<ScenarioDraftResponse>> {
  return request<ScenarioDraftResponse>(`/admin/scenarios/${scenarioId}`)
}

export function updateScenario(
  scenarioId: string,
  payload: ScenarioDraftEditPayload
): Promise<ApiResponse<ScenarioDraftResponse>> {
  return request<ScenarioDraftResponse>(`/admin/scenarios/${scenarioId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function approveScenario(
  scenarioId: string
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/scenarios/${scenarioId}/approve`, {
    method: 'POST',
  })
}

export function rejectScenario(
  scenarioId: string,
  data: ScenarioRejectRequest
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/scenarios/${scenarioId}/reject`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}
