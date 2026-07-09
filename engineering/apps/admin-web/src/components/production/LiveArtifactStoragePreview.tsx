/**
 * LIVE-003: Artifact Storage + Verification Preview — admin UI.
 * Internal opaque refs only. No storage URL/key/path.
 */

import type {
  TurkuLiveStorageResult,
  TurkuLiveStorageStatus,
  TurkuLiveStoredArtifact,
} from '../../types'

const STATUS_LABELS: Record<TurkuLiveStorageStatus, string> = {
  NOT_STORED: 'Depolanmadi',
  FAKE_STORED: 'Sahte Depolandi',
  REAL_STORED_INTERNAL: 'Gercek Dahili Depolandi',
  BLOCKED_BY_SAFETY: 'Guvenlik Engeli',
  WAITING_FOR_MANUAL_GATE: 'Manuel Kapi Bekleniyor',
  FAILED_SAFE: 'Basarisiz (Guvenli)',
}

export function renderStorageStatus(status: TurkuLiveStorageStatus): string {
  return STATUS_LABELS[status] || status
}

export function renderStoredArtifact(artifact: TurkuLiveStoredArtifact): string {
  const icon = artifact.verified ? '\u2705' : '\u274c'
  return `${icon} Sahne ${artifact.sceneNo}: ${artifact.artifactLabel} [${artifact.formatHint} ${artifact.widthHint}x${artifact.heightHint}]`
}

export function getStorageSummary(result: TurkuLiveStorageResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderStorageStatus(result.status)}`)
  lines.push(`Depolanan: ${result.storedCount}/${result.totalScenes}`)
  lines.push(`Dogrulanan: ${result.verifiedCount}/${result.totalScenes}`)
  lines.push(`Dogrulama: ${result.validationSummary}`)
  return lines.join('\n')
}

export function isAllVerified(result: TurkuLiveStorageResult): boolean {
  return result.verifiedCount === result.totalScenes && result.totalScenes > 0
}
