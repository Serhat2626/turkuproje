/**
 * MP-002: Work Order Preview — admin UI.
 * Non-executable checklist + gated work order metadata.
 */

import type {
  TurkuWorkOrderPreviewResult,
  TurkuWorkOrderPreviewStatus,
  TurkuProductionChecklistItem,
} from '../../types'

const STATUS_LABELS: Record<TurkuWorkOrderPreviewStatus, string> = {
  DRAFT_PREVIEW: 'Taslak \u00d6nizleme',
  BLOCKED_BY_SAFETY: 'G\u00fcvenlik Engeli',
  WAITING_FOR_MANUAL_REVIEW: 'Manuel \u0130nceleme Bekleniyor',
  ELIGIBLE_FOR_MANUAL_PREPARATION: 'Manuel Haz\u0131rl\u0131\u011fa Uygun',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

export function renderWorkOrderStatus(status: TurkuWorkOrderPreviewStatus): string {
  return STATUS_LABELS[status] || status
}

export function isEligibleForManualPreparation(result: TurkuWorkOrderPreviewResult): boolean {
  return result.status === 'ELIGIBLE_FOR_MANUAL_PREPARATION'
}

export function renderChecklistItem(item: TurkuProductionChecklistItem): string {
  const icon = item.prerequisiteMet ? '\u2705' : '\u274c'
  const lines = [`${icon} ${item.order}. ${item.label}`, `   Talimat: ${item.instruction}`]
  if (item.blockedReason) lines.push(`   Engel: ${item.blockedReason}`)
  return lines.join('\n')
}

export function getWorkOrderSummary(result: TurkuWorkOrderPreviewResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderWorkOrderStatus(result.status)}`)
  if (result.safeTurkuTitleLabel) lines.push(`T\u00fcrk\u00fc: ${result.safeTurkuTitleLabel}`)
  lines.push(`\u00d6zet: ${result.operatorSummary}`)
  lines.push(`Kontrol: ${result.completedItems}/${result.totalItems} tamamland\u0131`)
  if (result.blockedItems > 0) lines.push(`Engellenen: ${result.blockedItems}`)
  return lines.join('\n')
}

export function getWorkOrderSteps(result: TurkuWorkOrderPreviewResult): string {
  if (result.workOrderStepLabels.length === 0) return ''
  return result.workOrderStepLabels.map((s, i) => `${i + 1}. ${s}`).join('\n')
}

export function getWorkOrderBlockedReasons(result: TurkuWorkOrderPreviewResult): string {
  if (result.blockedReasons.length === 0) return 'Engel yok'
  return `Engeller: ${result.blockedReasons.join(', ')}`
}
