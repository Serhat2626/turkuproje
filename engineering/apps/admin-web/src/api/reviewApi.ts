import request from './client'
import type {
  ApiResponse,
  AdminReviewActionRequest,
  AdminReviewActionResponse,
  AdminReviewStateSummary,
  AdminReviewAuditTimelineItem,
  AdminReviewWorklistItem,
  AdminReviewTargetContext,
  AdminReviewActionAvailability,
  AdminReviewHandoffRequest,
  AdminReviewHandoffResponse,
  SafeAdminPreview,
  ReviewTargetType,
  ReviewState,
  ReviewActorRole,
} from '../types'

/**
 * FP-012: Admin Review Workflow API client.
 *
 * Explicit action methods only — no generic status update.
 * No publish/export/public URL/signed URL/CDN/provider/storage endpoint.
 * Do not log full request body, notes, or reasons.
 */

export function submitForReview(
  data: AdminReviewActionRequest
): Promise<ApiResponse<AdminReviewActionResponse>> {
  return request<AdminReviewActionResponse>('/admin/reviews/submit', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function startReview(
  data: AdminReviewActionRequest
): Promise<ApiResponse<AdminReviewActionResponse>> {
  return request<AdminReviewActionResponse>('/admin/reviews/start', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function requestChanges(
  data: AdminReviewActionRequest
): Promise<ApiResponse<AdminReviewActionResponse>> {
  return request<AdminReviewActionResponse>('/admin/reviews/request-changes', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function approveForInternalUse(
  data: AdminReviewActionRequest
): Promise<ApiResponse<AdminReviewActionResponse>> {
  return request<AdminReviewActionResponse>('/admin/reviews/approve-internal', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function rejectReview(
  data: AdminReviewActionRequest
): Promise<ApiResponse<AdminReviewActionResponse>> {
  return request<AdminReviewActionResponse>('/admin/reviews/reject', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function returnToDraft(
  data: AdminReviewActionRequest
): Promise<ApiResponse<AdminReviewActionResponse>> {
  return request<AdminReviewActionResponse>('/admin/reviews/return-to-draft', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function addReviewNote(
  data: AdminReviewActionRequest
): Promise<ApiResponse<AdminReviewActionResponse>> {
  return request<AdminReviewActionResponse>('/admin/reviews/note', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function getReviewState(
  targetType: ReviewTargetType,
  targetId: string
): Promise<ApiResponse<AdminReviewStateSummary>> {
  return request<AdminReviewStateSummary>(
    `/admin/reviews/${targetType}/${targetId}/state`
  )
}

export function getAuditTimeline(
  targetType: ReviewTargetType,
  targetId: string
): Promise<ApiResponse<AdminReviewAuditTimelineItem[]>> {
  return request<AdminReviewAuditTimelineItem[]>(
    `/admin/reviews/${targetType}/${targetId}/audit`
  )
}

// ── FP-012 Task 10: Worklist & Target Context ───────────────

export function getWorklist(
  params?: { state?: ReviewState; targetType?: ReviewTargetType; reviewer?: string; limit?: number }
): Promise<ApiResponse<AdminReviewWorklistItem[]>> {
  const query = new URLSearchParams()
  if (params?.state) query.set('state', params.state)
  if (params?.targetType) query.set('targetType', params.targetType)
  if (params?.reviewer) query.set('reviewer', params.reviewer)
  if (params?.limit) query.set('limit', String(params.limit))
  const qs = query.toString()
  return request<AdminReviewWorklistItem[]>(
    `/admin/reviews/worklist${qs ? '?' + qs : ''}`
  )
}

export function getTargetContext(
  targetType: ReviewTargetType,
  targetId: string
): Promise<ApiResponse<AdminReviewTargetContext>> {
  return request<AdminReviewTargetContext>(
    `/admin/reviews/${targetType}/${targetId}/context`
  )
}

// ── FP-012 Task 11: Action Availability ────────────────────

export function getAvailableActions(
  targetType: ReviewTargetType,
  targetId: string,
  actorRole: ReviewActorRole
): Promise<ApiResponse<AdminReviewActionAvailability>> {
  return request<AdminReviewActionAvailability>(
    `/admin/reviews/${targetType}/${targetId}/actions?actorRole=${actorRole}`
  )
}

// ── FP-013 Task 04: Handoff & Preview ────────────────────────

export function handoffReview(
  data: AdminReviewHandoffRequest
): Promise<ApiResponse<AdminReviewHandoffResponse>> {
  return request<AdminReviewHandoffResponse>('/admin/reviews/handoff', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function getPreview(
  targetType: ReviewTargetType,
  targetId: string
): Promise<ApiResponse<SafeAdminPreview>> {
  return request<SafeAdminPreview>(
    `/admin/reviews/${targetType}/${targetId}/preview`
  )
}
