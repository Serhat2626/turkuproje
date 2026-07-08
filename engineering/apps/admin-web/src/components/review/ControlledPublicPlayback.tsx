/**
 * PD-007: Controlled Public Playback MVP component.
 * Opaque playback handle — no direct URL/path/bucket exposure.
 */

import type {
  ProductionMediaPublicPlaybackResult,
  ProductionMediaPublicPlaybackStatus,
} from '../../types'

const STATUS_LABELS: Record<ProductionMediaPublicPlaybackStatus, string> = {
  CREATED_INTERNAL_ONLY: '\u0130\u00e7 Kullan\u0131m \u0130\u00e7in Olu\u015fturuldu',
  DELIVERY_GATE_ACCEPTED: 'Teslimat Kap\u0131s\u0131 Kabul Edildi',
  CONTROLLED_PLAYBACK_HANDLE_CREATED: 'Kontroll\u00fc Oynatma Olu\u015fturuldu',
  READY_FOR_CONTROLLED_PUBLIC_PLAYBACK: 'Kontroll\u00fc Herkese A\u00e7\u0131k Oynatma Haz\u0131r',
  REJECTED_UNSAFE: 'Reddedildi (G\u00fcvensiz)',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

export function renderPlaybackStatus(status: ProductionMediaPublicPlaybackStatus): string {
  return STATUS_LABELS[status] || status
}

export function isControlledPlaybackReady(result: ProductionMediaPublicPlaybackResult): boolean {
  return result.controlledPlaybackEligible === true
    && result.status === 'READY_FOR_CONTROLLED_PUBLIC_PLAYBACK'
}

export function getPlaybackMetadataSummary(result: ProductionMediaPublicPlaybackResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderPlaybackStatus(result.status)}`)
  lines.push(`Format: ${result.mediaFormat}`)
  lines.push(`S\u00fcre: ${result.durationSeconds}s`)
  lines.push(`\u00c7\u00f6z\u00fcn\u00fcrl\u00fck: ${result.width}x${result.height}`)
  lines.push(`FPS: ${result.fps}`)
  lines.push(`Boyut: ${result.sizeBytes} bayt`)
  lines.push(`Kontroll\u00fc Oynatma: ${result.controlledPlaybackEligible ? 'Evet' : 'Hay\u0131r'}`)

  if (result.opaquePublicPlaybackRef) {
    lines.push(`Oynatma Ref: ${result.opaquePublicPlaybackRef}`)
  }

  if (result.issueCodes.length > 0) {
    lines.push(`Sorun Kodlar\u0131: ${result.issueCodes.join(', ')}`)
  }

  return lines.join('\n')
}

export function getPlaybackPlaceholder(result: ProductionMediaPublicPlaybackResult): string {
  if (isControlledPlaybackReady(result)) {
    return `[Kontroll\u00fc Oynatma Haz\u0131r] ${result.mediaFormat} ${result.width}x${result.height} ${result.durationSeconds}s \u2014 Opak ref: ${result.opaquePublicPlaybackRef}`
  }
  return `[Oynatma Kullan\u0131lam\u0131yor] Durum: ${renderPlaybackStatus(result.status)}`
}
