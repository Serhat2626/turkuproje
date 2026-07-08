/**
 * RPE-003: Render Pipeline Configuration Preview — admin UI.
 * Render execution disabled. Metadata only.
 */

import type {
  TurkuRenderPipelineConfigResult,
  TurkuRenderConfigStatus,
  TurkuRenderEnvironmentBoundary,
  TurkuFfmpegCapabilityMetadata,
  TurkuOutputFormatPolicy,
} from '../../types'

const STATUS_LABELS: Record<TurkuRenderConfigStatus, string> = {
  NOT_CONFIGURED: 'Render Ortami Yapilandirilmadi',
  CONFIGURATION_DRAFT: 'Yapilandirma Taslagi',
  BLOCKED_BY_SAFETY: 'Guvenlik Engeli',
  WAITING_FOR_OPERATOR_REVIEW: 'Operator Incelemesi Bekleniyor',
  FAILED_SAFE: 'Basarisiz (Guvenli)',
}

export function renderRenderConfigStatus(status: TurkuRenderConfigStatus): string {
  return STATUS_LABELS[status] || status
}

export function isRenderExecutionEnabled(result: TurkuRenderPipelineConfigResult): boolean {
  return result.renderExecutionEnabled
}

export function renderEnvironment(env: TurkuRenderEnvironmentBoundary | null): string {
  if (!env) return 'Ortam bilgisi yok'
  const icon = env.environmentConfigured ? '\u2705' : '\u274c'
  return `${icon} ${env.environmentLabel}: ${env.configurationStatusLabel}`
}

export function renderFfmpegCapability(cap: TurkuFfmpegCapabilityMetadata): string {
  const icon = cap.available ? '\u2705' : '\u274c'
  return `${icon} ${cap.capabilityLabel}: ${cap.available ? 'Mevcut' : cap.availabilityNote}`
}

export function renderOutputPolicy(policy: TurkuOutputFormatPolicy | null): string {
  if (!policy) return 'Format politikasi belirlenmedi'
  return `${policy.formatLabel} ${policy.width}x${policy.height} @ ${policy.frameRate}fps`
}

export function getRenderConfigSummary(result: TurkuRenderPipelineConfigResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderRenderConfigStatus(result.status)}`)
  lines.push(`Dogrulama: ${result.validationSummary}`)
  return lines.join('\n')
}

export function getSafetyPrerequisites(result: TurkuRenderPipelineConfigResult): string {
  return result.safetyPrerequisiteLabels.map((s, i) => `${i + 1}. ${s}`).join('\n')
}
