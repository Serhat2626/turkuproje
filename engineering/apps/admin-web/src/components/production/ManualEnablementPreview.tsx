/**
 * MP-001: Manual Production Enablement Preview — admin UI.
 * Labels/instructions only. No executable provider/render actions.
 */

import type { TurkuManualEnablementResult, TurkuManualEnablementStatus } from '../../types'

const STATUS_LABELS: Record<TurkuManualEnablementStatus, string> = {
  NOT_ELIGIBLE: 'Uygun De\u011fil',
  ELIGIBLE_FOR_MANUAL_PREP: 'Manuel Haz\u0131rl\u0131\u011fa Uygun',
  BLOCKED_BY_SAFETY: 'G\u00fcvenlik Engeli',
  MANUAL_REVIEW_REQUIRED: 'Manuel \u0130nceleme Gerekli',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

export function renderEnablementStatus(status: TurkuManualEnablementStatus): string {
  return STATUS_LABELS[status] || status
}

export function isEligibleForManualPrep(result: TurkuManualEnablementResult): boolean {
  return result.status === 'ELIGIBLE_FOR_MANUAL_PREP'
}

export function getEnablementSummary(result: TurkuManualEnablementResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderEnablementStatus(result.status)}`)
  if (result.safeTurkuTitleLabel) lines.push(`T\u00fcrk\u00fc: ${result.safeTurkuTitleLabel}`)
  lines.push(`\u00d6zet: ${result.operatorSummary}`)
  return lines.join('\n')
}

export function getSafetyPrerequisites(result: TurkuManualEnablementResult): string {
  return result.safetyPrerequisites.map((s, i) => `${i + 1}. ${s}`).join('\n')
}

export function getPrerequisiteChecklist(result: TurkuManualEnablementResult): string {
  return [
    `Telif Niyeti: ${result.copyrightSafeIntentPresent ? '\u2705' : '\u274c'}`,
    `T\u00fcm Sahneler Planl\u0131: ${result.allScenesPlanned ? '\u2705' : '\u274c'}`,
    `Zaman \u00c7izelgesi Tam: ${result.timelineComplete ? '\u2705' : '\u274c'}`,
    `Engel Yok: ${result.noBlockedScenes ? '\u2705' : '\u274c'}`,
  ].join('\n')
}

export function getBlockedReasons(result: TurkuManualEnablementResult): string {
  if (result.blockedReasons.length === 0) return 'Engel yok'
  return `Engeller: ${result.blockedReasons.join(', ')}`
}

export function getManualSteps(result: TurkuManualEnablementResult): string {
  if (result.manualStepLabels.length === 0) return ''
  return result.manualStepLabels.map((s, i) => `${i + 1}. ${s}`).join('\n')
}
