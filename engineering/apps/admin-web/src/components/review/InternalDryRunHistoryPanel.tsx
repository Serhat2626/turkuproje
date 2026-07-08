/**
 * FP-051: Internal Dry Run History Panel — admin metadata-only.
 * No player/preview/download/export/publish controls.
 */

import type { TurkuInternalDryRunRecordSummary, TurkuInternalDryRunRecordStatus } from '../../types'

export function renderRecordStatus(status: TurkuInternalDryRunRecordStatus): string {
  const labels: Record<TurkuInternalDryRunRecordStatus, string> = {
    CREATED_INTERNAL_ONLY: 'Oluşturuldu (Dahili)',
    TRIGGER_ACCEPTED: 'Tetik Kabul Edildi',
    DRY_RUN_STARTED: 'Kuru Çalıştırma Başladı',
    METADATA_CAPTURED: 'Metadata Yakalandı',
    READY_FOR_ADMIN_HISTORY_ONLY: 'Admin Geçmişi İçin Hazır',
    REJECTED_UNSAFE: 'Güvensiz — Reddedildi',
    FAILED_SAFE: 'Güvenli Hata',
  }
  return labels[status] ?? status
}

export function isRecordSuccessful(record: TurkuInternalDryRunRecordSummary): boolean {
  return record.status === 'READY_FOR_ADMIN_HISTORY_ONLY'
}

export function getRecordMetadataSummary(record: TurkuInternalDryRunRecordSummary): string {
  const lines: string[] = [
    `Durum: ${renderRecordStatus(record.status)}`,
    `Kayıt Ref: ${record.opaqueRecordRef}`,
    `Sahne: ${record.sceneCount}`,
    `Üretilen Klip: ${record.generatedClipCount}`,
    `Girdi Klip: ${record.inputClipCount}`,
    `Birleştirilmiş: ${record.stitchedArtifactPresent ? 'Evet' : 'Hayır'}`,
    `Süre: ${record.durationSeconds}s`,
    `Çözünürlük: ${record.width}×${record.height}`,
    `FPS: ${record.fps}`,
    `Boyut: ${(record.sizeBytes / 1024 / 1024).toFixed(1)} MB`,
    `Dahili: ${record.internalOnly ? 'Evet' : 'Hayır'}`,
  ]
  if (record.issueCodes.length > 0) {
    lines.push(`Sorunlar: ${record.issueCodes.join(', ')}`)
  }
  return lines.join('\n')
}

export function getHistorySummary(records: TurkuInternalDryRunRecordSummary[]): string {
  if (records.length === 0) return 'Kayıt yok'
  return records
    .map((r, i) => `#${i + 1}: ${renderRecordStatus(r.status)} — Sahne: ${r.sceneCount}`)
    .join('\n')
}
