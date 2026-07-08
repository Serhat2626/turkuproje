/**
 * PD-005: Admin Review Decision Workflow UI.
 * Internal-only. No publish, no auto-approval, no public delivery.
 */

import type {
  ProductionMediaReviewDecisionResult,
  ProductionMediaReviewDecisionStatus,
  ProductionMediaReviewDecisionType,
} from '../../types'

const STATUS_LABELS: Record<ProductionMediaReviewDecisionStatus, string> = {
  CREATED_INTERNAL_ONLY: '\u0130\u00e7 Kullan\u0131m \u0130\u00e7in Olu\u015fturuldu',
  REVIEW_METADATA_ACCEPTED: '\u0130nceleme Metadatas\u0131 Kabul Edildi',
  DECISION_RECORDED_INTERNAL_ONLY: 'Karar Kaydedildi (Dahili)',
  APPROVED_FOR_INTERNAL_DELIVERY_PREP: 'Dahili Teslimat Haz\u0131rl\u0131\u011f\u0131 Onayland\u0131',
  REJECTED_FOR_INTERNAL_REVISION: 'Dahili Revizyon \u0130\u00e7in Reddedildi',
  INTERNAL_FIX_REQUESTED: 'Dahili D\u00fczeltme Talep Edildi',
  REJECTED_UNSAFE: 'Reddedildi (G\u00fcvensiz)',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

const DECISION_LABELS: Record<ProductionMediaReviewDecisionType, string> = {
  APPROVE_FOR_INTERNAL_DELIVERY_PREP: 'Dahili Teslimat Haz\u0131rl\u0131\u011f\u0131n\u0131 Onayla',
  REJECT_FOR_INTERNAL_REVISION: 'Dahili Revizyon \u0130\u00e7in Reddet',
  REQUEST_INTERNAL_FIX: 'Dahili D\u00fczeltme Talep Et',
  MARK_UNSAFE_INTERNAL_ONLY: 'G\u00fcvensiz Olarak \u0130\u015faretle',
}

export function renderDecisionStatus(status: ProductionMediaReviewDecisionStatus): string {
  return STATUS_LABELS[status] || status
}

export function renderDecisionLabel(type: ProductionMediaReviewDecisionType): string {
  return DECISION_LABELS[type] || type
}

export function isDecisionApproved(result: ProductionMediaReviewDecisionResult): boolean {
  return result.status === 'APPROVED_FOR_INTERNAL_DELIVERY_PREP'
    && result.internalOnly === true
}

export function getDecisionSummary(result: ProductionMediaReviewDecisionResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderDecisionStatus(result.status)}`)
  if (result.decisionType) {
    lines.push(`Karar: ${renderDecisionLabel(result.decisionType)}`)
  }
  lines.push(`Dahili: ${result.internalOnly ? 'Evet' : 'Hay\u0131r'}`)
  if (result.issueCodes.length > 0) {
    lines.push(`Sorun Kodlar\u0131: ${result.issueCodes.join(', ')}`)
  }
  return lines.join('\n')
}

export function getDecisionControls(): { type: ProductionMediaReviewDecisionType; label: string }[] {
  return [
    { type: 'APPROVE_FOR_INTERNAL_DELIVERY_PREP', label: DECISION_LABELS.APPROVE_FOR_INTERNAL_DELIVERY_PREP },
    { type: 'REJECT_FOR_INTERNAL_REVISION', label: DECISION_LABELS.REJECT_FOR_INTERNAL_REVISION },
    { type: 'REQUEST_INTERNAL_FIX', label: DECISION_LABELS.REQUEST_INTERNAL_FIX },
    { type: 'MARK_UNSAFE_INTERNAL_ONLY', label: DECISION_LABELS.MARK_UNSAFE_INTERNAL_ONLY },
  ]
}
