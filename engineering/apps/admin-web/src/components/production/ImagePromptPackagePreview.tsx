/**
 * RPE-005: Image Prompt Package Preview — admin UI.
 * No provider call, no image generation. Safe summaries only.
 */

import type {
  TurkuImagePromptPackageResult,
  TurkuImagePromptPackageStatus,
  TurkuSceneImagePrompt,
} from '../../types'

const STATUS_LABELS: Record<TurkuImagePromptPackageStatus, string> = {
  DRAFT: 'Taslak',
  PENDING_MANUAL_APPROVAL: 'Manuel Onay Bekleniyor',
  BLOCKED_BY_SAFETY: 'Guvenlik Engeli',
  WAITING_FOR_OPERATOR_REVIEW: 'Operator Incelemesi Bekleniyor',
  FAILED_SAFE: 'Basarisiz (Guvenli)',
}

export function renderPromptPackageStatus(status: TurkuImagePromptPackageStatus): string {
  return STATUS_LABELS[status] || status
}

export function isManualApprovalGranted(result: TurkuImagePromptPackageResult): boolean {
  return result.manualApprovalGranted
}

export function renderScenePrompt(prompt: TurkuSceneImagePrompt): string {
  const icon = prompt.blocked ? '\u274c' : '\u2705'
  const blocked = prompt.blocked ? ` — ${prompt.blockedReason}` : ''
  return `${icon} Sahne ${prompt.sceneNo}: ${prompt.safePromptSummary} [${prompt.aspectRatio}]${blocked}`
}

export function getPromptPackageSummary(result: TurkuImagePromptPackageResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderPromptPackageStatus(result.status)}`)
  if (result.safeTurkuTitleLabel) lines.push(`Turku: ${result.safeTurkuTitleLabel}`)
  lines.push(`Sahneler: ${result.totalScenes} toplam, ${result.blockedScenes} engelli`)
  lines.push(`Onay: ${result.approvalGateLabel}`)
  lines.push(`Dogrulama: ${result.validationSummary}`)
  return lines.join('\n')
}

export function getSafetyConstraints(prompt: TurkuSceneImagePrompt): string {
  return prompt.safetyConstraintLabels.map((s, i) => `${i + 1}. ${s}`).join('\n')
}
