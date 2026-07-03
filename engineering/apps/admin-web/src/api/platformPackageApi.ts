import request from './client'
import type {
  ApiResponse,
  ActionStatusResponse,
  PlatformPackageDraftSummaryResponse,
  PlatformPackageDraftResponse,
  PlatformPackageDraftEditPayload,
  PlatformPackageGenerateResponse,
  PlatformPackageRejectRequest,
  PlatformPackageExportResponse,
} from '../types'
import type { PlatformPackageExportType } from '../types'

/**
 * FP-004: Platform Package Workflow API çağrıları.
 * Generic status update yok — sadece action endpoint'ler.
 * Internal AI contract DTO'ları expose edilmez.
 */

export function generatePlatformPackagesForStoryboard(
  storyboardId: string
): Promise<ApiResponse<PlatformPackageGenerateResponse>> {
  return request<PlatformPackageGenerateResponse>(`/admin/storyboards/${storyboardId}/platform-packages/generate`, {
    method: 'POST',
  })
}

export function listPlatformPackagesByContentProject(
  contentProjectId: string
): Promise<ApiResponse<PlatformPackageDraftSummaryResponse[]>> {
  return request<PlatformPackageDraftSummaryResponse[]>(`/admin/content-projects/${contentProjectId}/platform-packages`)
}

export function listPlatformPackagesByStoryboard(
  storyboardId: string
): Promise<ApiResponse<PlatformPackageDraftSummaryResponse[]>> {
  return request<PlatformPackageDraftSummaryResponse[]>(`/admin/storyboards/${storyboardId}/platform-packages`)
}

export function getPlatformPackageDetail(
  packageId: string
): Promise<ApiResponse<PlatformPackageDraftResponse>> {
  return request<PlatformPackageDraftResponse>(`/admin/platform-packages/${packageId}`)
}

export function updatePlatformPackageDraft(
  packageId: string,
  payload: PlatformPackageDraftEditPayload
): Promise<ApiResponse<PlatformPackageDraftResponse>> {
  return request<PlatformPackageDraftResponse>(`/admin/platform-packages/${packageId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function approvePlatformPackageDraft(
  packageId: string
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/platform-packages/${packageId}/approve`, {
    method: 'POST',
  })
}

export function rejectPlatformPackageDraft(
  packageId: string,
  data: PlatformPackageRejectRequest
): Promise<ApiResponse<ActionStatusResponse>> {
  return request<ActionStatusResponse>(`/admin/platform-packages/${packageId}/reject`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

/**
 * FP-005: Platform paketini belirtilen formatta export et.
 * Read-only — status değiştirmez, side-effect yok.
 */
export function exportPlatformPackage(
  packageId: string,
  exportType: PlatformPackageExportType
): Promise<ApiResponse<PlatformPackageExportResponse>> {
  return request<PlatformPackageExportResponse>(
    `/admin/platform-packages/${packageId}/exports/${exportType}`
  )
}
