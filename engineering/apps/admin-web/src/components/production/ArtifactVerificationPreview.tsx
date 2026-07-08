/**
 * RPE-007: Artifact Verification + Render Package Preview — admin UI.
 * No render/FFmpeg/storage execution.
 */

import type {
  TurkuArtifactVerificationReport,
  TurkuArtifactVerificationStatus,
  TurkuRenderPackageResult,
  TurkuRenderPackageStatus,
  TurkuSceneArtifactReadiness,
} from '../../types'

const VERF_LABELS: Record<TurkuArtifactVerificationStatus, string> = {
  NOT_VERIFIED: 'Dogrulanmadi',
  FAKE_VERIFIED: 'Sahte Dogrulandi',
  BLOCKED_BY_SAFETY: 'Guvenlik Engeli',
  WAITING_FOR_OPERATOR_REVIEW: 'Operator Incelemesi Bekleniyor',
  FAILED_SAFE: 'Basarisiz (Guvenli)',
}

const RPKG_LABELS: Record<TurkuRenderPackageStatus, string> = {
  NOT_READY: 'Hazir Degil',
  DRAFT: 'Taslak',
  BLOCKED_BY_SAFETY: 'Guvenlik Engeli',
  WAITING_FOR_MANUAL_GATE: 'Manuel Kapi Bekleniyor',
  FAILED_SAFE: 'Basarisiz (Guvenli)',
}

export function renderVerificationStatus(status: TurkuArtifactVerificationStatus): string {
  return VERF_LABELS[status] || status
}

export function renderRenderPackageStatus(status: TurkuRenderPackageStatus): string {
  return RPKG_LABELS[status] || status
}

export function renderSceneReadiness(scene: TurkuSceneArtifactReadiness): string {
  const icon = scene.verified ? '\u2705' : '\u274c'
  return `${icon} Sahne ${scene.sceneNo}: ${scene.artifactLabel} — ${scene.verificationNote}`
}

export function getVerificationSummary(report: TurkuArtifactVerificationReport): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderVerificationStatus(report.status)}`)
  lines.push(`Sahneler: ${report.verifiedScenes}/${report.totalScenes} dogrulandi`)
  lines.push(`Dogrulama: ${report.validationSummary}`)
  return lines.join('\n')
}

export function getRenderPackageSummary(result: TurkuRenderPackageResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderRenderPackageStatus(result.status)}`)
  lines.push(`Manifest: ${result.renderInputManifestSummary}`)
  lines.push(`Yerel render: ${result.localManualRenderEnabled ? 'Etkin' : 'Devre disi'}`)
  lines.push(`Dogrulama: ${result.validationSummary}`)
  return lines.join('\n')
}

export function isLocalRenderEnabled(result: TurkuRenderPackageResult): boolean {
  return result.localManualRenderEnabled
}
