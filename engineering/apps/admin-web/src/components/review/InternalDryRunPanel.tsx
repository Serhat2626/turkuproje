/**
 * FP-049: Admin Internal Dry Run Panel.
 * Metadata-only. No player/preview/download/export/publish controls.
 * No public URL/link rendering. No unsafe HTML.
 */

import type {
  TurkuInternalDryRunTriggerResponse,
  TurkuStoryboardInternalRenderChainStatus,
} from '../../types'

const SAFE_STATUS_LABELS: Record<TurkuStoryboardInternalRenderChainStatus, string> = {
  CREATED_INTERNAL_ONLY: 'Oluşturuldu (Dahili)',
  STORYBOARD_ACCEPTED: 'Storyboard Kabul Edildi',
  SCENE_PLANS_CREATED: 'Sahne Planları Oluşturuldu',
  MULTI_SCENE_JOB_READY: 'Çoklu Sahne İşi Hazır',
  SYNTHETIC_CLIPS_GENERATED: 'Sentetik Klipler Üretildi',
  STITCHED_ARTIFACT_VERIFIED: 'Birleştirilmiş Artifact Doğrulandı',
  REVIEW_PACKAGE_READY_INTERNAL_ONLY: 'İnceleme Paketi Hazır (Dahili)',
  ADMIN_METADATA_VISIBLE: 'Admin Metadata Görünür',
  REJECTED_UNSAFE: 'Güvensiz — Reddedildi',
  FAILED_SAFE: 'Güvenli Hata',
}

export function renderDryRunStatus(status: TurkuStoryboardInternalRenderChainStatus): string {
  return SAFE_STATUS_LABELS[status] ?? status
}

export function isDryRunSuccessful(response: TurkuInternalDryRunTriggerResponse): boolean {
  return response.status === 'ADMIN_METADATA_VISIBLE' && response.internalOnly === true
}

export function getDryRunMetadataSummary(response: TurkuInternalDryRunTriggerResponse): string {
  const lines: string[] = [
    `Durum: ${renderDryRunStatus(response.status)}`,
    `Sahne: ${response.sceneCount}`,
    `Klip: ${response.generatedClipCount}`,
    `Birleştirildi: ${response.stitchedArtifactPresent ? 'Evet' : 'Hayır'}`,
    `İnceleme: ${response.reviewPackageStatus ?? '—'}`,
    `Admin Görünür: ${response.adminMetadataVisible ? 'Evet' : 'Hayır'}`,
    `Süre: ${response.totalDurationSeconds}s`,
    `Çözünürlük: ${response.maxWidth}×${response.maxHeight}`,
    `FPS: ${response.maxFps}`,
    `Boyut: ${response.totalSizeBytes} bytes`,
    `Dahili: ${response.internalOnly ? 'Evet' : 'Hayır'}`,
  ]
  if (response.issueCodes.length > 0) {
    lines.push(`Sorunlar: ${response.issueCodes.join(', ')}`)
  }
  return lines.join('\n')
}

// Forbidden controls — explicitly absent
// No player/preview/download/export/publish/public URL controls
