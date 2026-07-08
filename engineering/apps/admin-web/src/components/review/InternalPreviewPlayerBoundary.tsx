/**
 * PD-004: Admin Internal Preview Player Boundary.
 * Metadata-only — no real video stream, no public URL, no download.
 */

import type { ProductionMediaPlaybackPreparationResult, ProductionMediaPlaybackPreparationStatus } from '../../types'

// ── Status Rendering ──

const STATUS_LABELS: Record<ProductionMediaPlaybackPreparationStatus, string> = {
  CREATED_INTERNAL_ONLY: '\u0130\u00e7 Kullan\u0131m \u0130\u00e7in Olu\u015fturuldu',
  PRIVATE_STORAGE_REF_ACCEPTED: '\u00d6zel Depolama Kabul Edildi',
  PLAYBACK_METADATA_PREPARED_INTERNAL_ONLY: 'Oynatma Metadata Haz\u0131rland\u0131',
  READY_FOR_INTERNAL_PREVIEW_ONLY: 'Dahili \u00d6nizleme Haz\u0131r',
  REJECTED_UNSAFE: 'Reddedildi (G\u00fcvensiz)',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

export function renderPreviewStatus(status: ProductionMediaPlaybackPreparationStatus): string {
  return STATUS_LABELS[status] || status
}

// ── Eligibility Check ──

export function isInternalPreviewEligible(result: ProductionMediaPlaybackPreparationResult): boolean {
  return result.internalPreviewEligible === true
    && result.status === 'READY_FOR_INTERNAL_PREVIEW_ONLY'
}

// ── Metadata Summary ──

export function getPreviewMetadataSummary(result: ProductionMediaPlaybackPreparationResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderPreviewStatus(result.status)}`)
  lines.push(`Format: ${result.mediaFormat}`)
  lines.push(`S\u00fcre: ${result.durationSeconds}s`)
  lines.push(`\u00c7\u00f6z\u00fcn\u00fcrl\u00fck: ${result.width}x${result.height}`)
  lines.push(`FPS: ${result.fps}`)
  lines.push(`Boyut: ${result.sizeBytes} bayt`)
  lines.push(`Dahili \u00d6nizleme: ${result.internalPreviewEligible ? 'Evet' : 'Hay\u0131r'}`)

  if (result.issueCodes.length > 0) {
    lines.push(`Sorun Kodlar\u0131: ${result.issueCodes.join(', ')}`)
  }

  return lines.join('\n')
}

// ── Preview Placeholder ──

export function getPreviewPlaceholder(result: ProductionMediaPlaybackPreparationResult): string {
  if (isInternalPreviewEligible(result)) {
    return `[Dahili \u00d6nizleme Haz\u0131r] ${result.mediaFormat} ${result.width}x${result.height} ${result.durationSeconds}s — Yaln\u0131zca dahili kullan\u0131m i\u00e7indir`
  }
  return `[\u00d6nizleme Kullan\u0131lam\u0131yor] Durum: ${renderPreviewStatus(result.status)}`
}
