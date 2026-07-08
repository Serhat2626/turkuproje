/**
 * RPE-004: Execution Package Preview — admin UI.
 * All execution disabled. Manual gates metadata only.
 */

import type {
  TurkuExecutionPackageResult,
  TurkuExecutionPackageStatus,
  TurkuManualGate,
  TurkuExecutionCapability,
} from '../../types'

const STATUS_LABELS: Record<TurkuExecutionPackageStatus, string> = {
  PACKAGE_DRAFT: 'Paket Taslagi',
  NOT_READY: 'Hazir Degil',
  BLOCKED_BY_SAFETY: 'Guvenlik Engeli',
  WAITING_FOR_MANUAL_GATE: 'Manuel Kapi Bekleniyor',
  FAILED_SAFE: 'Basarisiz (Guvenli)',
}

export function renderPackageStatus(status: TurkuExecutionPackageStatus): string {
  return STATUS_LABELS[status] || status
}

export function isAnyCapabilityEnabled(result: TurkuExecutionPackageResult): boolean {
  return result.executionCapabilities.some(c => c.enabled)
}

export function renderManualGate(gate: TurkuManualGate): string {
  const icon = gate.passed ? '\u2705' : '\u274c'
  const blocked = !gate.passed && gate.blockedReason ? ` — ${gate.blockedReason}` : ''
  return `${icon} ${gate.order}. ${gate.gateLabel}: ${gate.instruction}${blocked}`
}

export function renderCapabilityFlag(cap: TurkuExecutionCapability): string {
  const icon = cap.enabled ? '\u2705' : '\u274c'
  return `${icon} ${cap.capabilityLabel}: ${cap.enabled ? 'Etkin' : cap.disabledReason}`
}

export function getPackageSummary(result: TurkuExecutionPackageResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderPackageStatus(result.status)}`)
  if (result.safeTurkuTitleLabel) lines.push(`Turku: ${result.safeTurkuTitleLabel}`)
  lines.push(`Saglayici: ${result.providerConfigSummary}`)
  lines.push(`Render: ${result.renderConfigSummary}`)
  lines.push(`Dogrulama: ${result.validationSummary}`)
  return lines.join('\n')
}

export function getFutureStepLabels(result: TurkuExecutionPackageResult): string {
  return result.futureExecutionStepLabels.map((s, i) => `${i + 1}. ${s}`).join('\n')
}

export function getBlockedReasons(result: TurkuExecutionPackageResult): string {
  if (result.blockedReasons.length === 0) return 'Engel yok'
  return `Engeller: ${result.blockedReasons.join(', ')}`
}
