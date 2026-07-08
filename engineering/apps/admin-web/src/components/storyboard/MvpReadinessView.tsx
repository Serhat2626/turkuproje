/**
 * RP-005: MVP Readiness Package View — admin UI.
 * Safe readiness summary. No render/provider/publish actions.
 */

import type { TurkuMvpReadinessResult, TurkuMvpReadinessStatus } from '../../types'

const STATUS_LABELS: Record<TurkuMvpReadinessStatus, string> = {
  NOT_READY: 'Haz\u0131r De\u011fil',
  READY_FOR_MANUAL_REVIEW: 'Manuel \u0130ncelemeye Haz\u0131r',
  BLOCKED_BY_SAFETY: 'G\u00fcvenlik Engeli',
  PARTIALLY_READY: 'K\u0131smen Haz\u0131r',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

export function renderReadinessStatus(status: TurkuMvpReadinessStatus): string {
  return STATUS_LABELS[status] || status
}

export function isReadyForManualReview(result: TurkuMvpReadinessResult): boolean {
  return result.status === 'READY_FOR_MANUAL_REVIEW'
}

export function getReadinessSummary(result: TurkuMvpReadinessResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderReadinessStatus(result.status)}`)
  if (result.safeTurkuTitleLabel) lines.push(`T\u00fcrk\u00fc: ${result.safeTurkuTitleLabel}`)
  lines.push(`Storyboard: ${result.storyboardReadinessSummary}`)
  lines.push(`Varl\u0131k Plan\u0131: ${result.assetPlanReadinessSummary}`)
  lines.push(`Zaman \u00c7izelgesi: ${result.timelineReadinessSummary}`)
  lines.push(`Toplam Sahne: ${result.totalScenes}`)
  lines.push(`Engellenen: ${result.blockedScenes}`)
  lines.push(`S\u00fcre: ${result.totalDurationMs}ms`)
  return lines.join('\n')
}

export function getSafetyChecklist(result: TurkuMvpReadinessResult): string {
  const items = [
    `Telif Niyeti: ${result.copyrightSafeIntentPresent ? '\u2705' : '\u274c'}`,
    `T\u00fcm Sahneler Planl\u0131: ${result.allScenesPlanned ? '\u2705' : '\u274c'}`,
    `Zaman \u00c7izelgesi Tam: ${result.timelineComplete ? '\u2705' : '\u274c'}`,
    `Engel Yok: ${result.noBlockedScenes ? '\u2705' : '\u274c'}`,
  ]
  return items.join('\n')
}

export function getBlockingIssues(result: TurkuMvpReadinessResult): string {
  if (result.blockingIssues.length === 0) return 'Engel yok'
  return `Engeller: ${result.blockingIssues.join(', ')}`
}

export function getNextSteps(result: TurkuMvpReadinessResult): string {
  if (result.nextAllowedManualStepLabels.length === 0) return ''
  return result.nextAllowedManualStepLabels.map((s, i) => `${i + 1}. ${s}`).join('\n')
}
