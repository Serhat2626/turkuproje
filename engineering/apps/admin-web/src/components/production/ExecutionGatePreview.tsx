/**
 * RPE-001: Execution Safety Gate Preview — admin UI.
 * All capabilities disabled. Labels/instructions only.
 */

import type {
  TurkuExecutionGateResult,
  TurkuExecutionGateStatus,
  TurkuExecutionCapability,
} from '../../types'

const STATUS_LABELS: Record<TurkuExecutionGateStatus, string> = {
  EXECUTION_NOT_ENABLED: 'Y\u00fcr\u00fctme Etkinle\u015ftirilmedi',
  BLOCKED_BY_SAFETY: 'G\u00fcvenlik Engeli',
  WAITING_FOR_OPERATOR_GATE: 'Operat\u00f6r Kap\u0131s\u0131 Bekleniyor',
  ELIGIBLE_FOR_FUTURE_EXECUTION_REVIEW: 'Gelecek Y\u00fcr\u00fctme \u0130ncelemesine Uygun',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

export function renderGateStatus(status: TurkuExecutionGateStatus): string {
  return STATUS_LABELS[status] || status
}

export function isExecutionEnabled(result: TurkuExecutionGateResult): boolean {
  return result.capabilities.some(c => c.enabled)
}

export function renderCapability(cap: TurkuExecutionCapability): string {
  const icon = cap.enabled ? '\u2705' : '\u274c'
  return `${icon} ${cap.capabilityLabel}: ${cap.enabled ? 'Etkin' : cap.disabledReason}`
}

export function getGateSummary(result: TurkuExecutionGateResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderGateStatus(result.status)}`)
  if (result.safeTurkuTitleLabel) lines.push(`T\u00fcrk\u00fc: ${result.safeTurkuTitleLabel}`)
  lines.push(`Denetim: ${result.auditDecisionSummary}`)
  return lines.join('\n')
}

export function getOperatorGates(result: TurkuExecutionGateResult): string {
  return result.requiredOperatorGates.map((g, i) => `${i + 1}. ${g}`).join('\n')
}

export function getFutureSteps(result: TurkuExecutionGateResult): string {
  return result.futureExecutionStepLabels.map((s, i) => `${i + 1}. ${s}`).join('\n')
}

export function getGateBlockedReasons(result: TurkuExecutionGateResult): string {
  if (result.blockedReasons.length === 0) return 'Engel yok'
  return `Engeller: ${result.blockedReasons.join(', ')}`
}
