/**
 * RP-001: Türkü Name Storyboard Request Form.
 * No lyrics, no upload, no URL input, no player/download/export/publish.
 */

import type {
  TurkuNameStoryboardRequestResult,
  TurkuNameStoryboardRequestStatus,
} from '../../types'

const STATUS_LABELS: Record<TurkuNameStoryboardRequestStatus, string> = {
  CREATED_INTERNAL_ONLY: '\u0130\u00e7 Kullan\u0131m \u0130\u00e7in Olu\u015fturuldu',
  TURKU_NAME_ACCEPTED: 'T\u00fcrk\u00fc Ad\u0131 Kabul Edildi',
  COPYRIGHT_SAFE_INTENT_CREATED: 'Telif G\u00fcvenli Niyet Olu\u015fturuldu',
  READY_FOR_STORYBOARD_GENERATION_BOUNDARY: 'Storyboard Olu\u015fturmaya Haz\u0131r',
  REJECTED_UNSAFE: 'Reddedildi (G\u00fcvensiz)',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

export function renderRequestStatus(status: TurkuNameStoryboardRequestStatus): string {
  return STATUS_LABELS[status] || status
}

export function isRequestReady(result: TurkuNameStoryboardRequestResult): boolean {
  return result.status === 'READY_FOR_STORYBOARD_GENERATION_BOUNDARY'
}

export function getRequestSummary(result: TurkuNameStoryboardRequestResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderRequestStatus(result.status)}`)
  if (result.safeTurkuTitleLabel) {
    lines.push(`T\u00fcrk\u00fc: ${result.safeTurkuTitleLabel}`)
  }
  if (result.inspirationMode) {
    lines.push(`\u0130lham Modu: ${result.inspirationMode}`)
  }
  lines.push(`Sahne: ${result.targetSceneCount}`)
  lines.push(`S\u00fcre: ${result.targetDurationMs}ms`)
  lines.push(`\u00c7\u00f6z\u00fcn\u00fcrl\u00fck: ${result.targetWidth}x${result.targetHeight}`)
  lines.push(`FPS: ${result.targetFps}`)
  if (result.copyrightSafeIntentSummary) {
    lines.push(`Telif Niyeti: ${result.copyrightSafeIntentSummary}`)
  }
  if (result.issueCodes.length > 0) {
    lines.push(`Sorun Kodlar\u0131: ${result.issueCodes.join(', ')}`)
  }
  return lines.join('\n')
}

export function getInputWarning(): string {
  return '\u26a0\ufe0f S\u00f6z, ses dosyas\u0131, video veya medya URL\u2019si kabul edilmez. Yaln\u0131zca t\u00fcrk\u00fc ad\u0131 giriniz.'
}
