/**
 * LIVE-005: Human Review + Controlled Delivery Preview — admin UI.
 * No public URL/storage key/path. Admin-only controlled delivery.
 */

import type {
  TurkuLiveControlledDeliveryResult,
  TurkuLiveDeliveryStatus,
  TurkuLiveReviewDecision,
  TurkuLiveReviewChecklistItem,
} from '../../types'

const STATUS_LABELS: Record<TurkuLiveDeliveryStatus, string> = {
  NOT_ELIGIBLE: 'Uygun Degil',
  PENDING_HUMAN_REVIEW: 'Insan Incelemesi Bekleniyor',
  ELIGIBLE_FOR_CONTROLLED_DELIVERY: 'Kontrollu Teslim Uygun',
  BLOCKED_BY_REVIEW: 'Inceleme Engeli',
  BLOCKED_BY_SAFETY: 'Guvenlik Engeli',
  FAILED_SAFE: 'Basarisiz (Guvenli)',
}

const DECISION_LABELS: Record<TurkuLiveReviewDecision, string> = {
  PENDING: 'Beklemede',
  APPROVED_FOR_CONTROLLED_DELIVERY: 'Onaylandi (Kontrollu Teslim)',
  REJECTED: 'Reddedildi',
  NEEDS_REVISION: 'Revizyon Gerekli',
}

export function renderDeliveryStatus(status: TurkuLiveDeliveryStatus): string {
  return STATUS_LABELS[status] || status
}

export function renderReviewDecision(decision: TurkuLiveReviewDecision): string {
  return DECISION_LABELS[decision] || decision
}

export function renderChecklistItem(item: TurkuLiveReviewChecklistItem): string {
  const icon = item.checked ? '\u2705' : '\u2B1C'
  return `${icon} ${item.label}`
}

export function getDeliverySummary(result: TurkuLiveControlledDeliveryResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderDeliveryStatus(result.status)}`)
  lines.push(`Inceleme: ${renderReviewDecision(result.reviewDecision)}`)
  lines.push(`Render: ${result.renderArtifactLabel} [${result.formatHint} ${result.resolutionHint}]`)
  lines.push(`Teslim Uygunlugu: ${result.exportEligible ? 'Evet' : 'Hayir'}`)
  lines.push(`Dogrulama: ${result.validationSummary}`)
  return lines.join('\n')
}

export function isExportEligible(result: TurkuLiveControlledDeliveryResult): boolean {
  return result.exportEligible && result.reviewDecision === 'APPROVED_FOR_CONTROLLED_DELIVERY'
}
