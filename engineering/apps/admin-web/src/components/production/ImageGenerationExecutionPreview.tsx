/**
 * RPE-006: Image Generation Execution Preview — admin UI.
 * CI/default = CI_FAKE. Real provider disabled.
 */

import type {
  TurkuImageGenerationExecutionResult,
  TurkuImageGenerationStatus,
  TurkuImageGenerationExecutionMode,
  TurkuFakeImageResultMetadata,
} from '../../types'

const STATUS_LABELS: Record<TurkuImageGenerationStatus, string> = {
  NOT_STARTED: 'Baslamadi',
  FAKE_RESULT_AVAILABLE: 'Sahte Sonuc Mevcut',
  BLOCKED_BY_SAFETY: 'Guvenlik Engeli',
  WAITING_FOR_MANUAL_GATE: 'Manuel Kapi Bekleniyor',
  FAILED_SAFE: 'Basarisiz (Guvenli)',
}

const MODE_LABELS: Record<TurkuImageGenerationExecutionMode, string> = {
  DISABLED: 'Devre Disi',
  CI_FAKE: 'CI Sahte Mod',
  LOCAL_MANUAL_ONLY: 'Yerel/Manuel',
}

export function renderGenerationStatus(status: TurkuImageGenerationStatus): string {
  return STATUS_LABELS[status] || status
}

export function renderExecutionMode(mode: TurkuImageGenerationExecutionMode): string {
  return MODE_LABELS[mode] || mode
}

export function renderFakeResult(result: TurkuFakeImageResultMetadata): string {
  return `Sahne ${result.sceneNo}: ${result.artifactLabel} [${result.formatHint} ${result.widthHint}x${result.heightHint}]`
}

export function getGenerationSummary(result: TurkuImageGenerationExecutionResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderGenerationStatus(result.status)}`)
  lines.push(`Mod: ${renderExecutionMode(result.executionMode)}`)
  lines.push(`Sonuclar: ${result.fakeResultCount}/${result.totalScenes} sahte gorsel`)
  lines.push(`Dogrulama: ${result.validationSummary}`)
  return lines.join('\n')
}

export function isRealProviderEnabled(result: TurkuImageGenerationExecutionResult): boolean {
  return result.executionMode !== 'DISABLED' && result.executionMode !== 'CI_FAKE'
}
