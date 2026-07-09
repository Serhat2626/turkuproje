/**
 * LIVE-002: Live Image Generation Preview — admin UI.
 * Shows gate status, generation mode, sanitized results.
 */

import type {
  TurkuLiveGenerationResult,
  TurkuLiveGenerationStatus,
  TurkuLiveGenerationMode,
  TurkuLiveImageResultMetadata,
} from '../../types'

const STATUS_LABELS: Record<TurkuLiveGenerationStatus, string> = {
  NOT_STARTED: 'Baslamadi',
  FAKE_RESULT_AVAILABLE: 'Sahte Sonuc Mevcut',
  REAL_PROVIDER_RESULT_AVAILABLE: 'Gercek Saglayici Sonucu Mevcut',
  BLOCKED_BY_KILL_SWITCH: 'Kill Switch Engeli',
  BLOCKED_BY_OPERATOR_GATE: 'Operator Kapisi Bekleniyor',
  BLOCKED_BY_PROVIDER_READINESS: 'Saglayici Hazir Degil',
  FAILED_SAFE: 'Basarisiz (Guvenli)',
}

const MODE_LABELS: Record<TurkuLiveGenerationMode, string> = {
  CI_FAKE: 'CI Sahte Mod',
  LOCAL_MANUAL_REAL_PROVIDER: 'Yerel/Manuel Gercek Saglayici',
}

export function renderLiveGenStatus(status: TurkuLiveGenerationStatus): string {
  return STATUS_LABELS[status] || status
}

export function renderLiveGenMode(mode: TurkuLiveGenerationMode): string {
  return MODE_LABELS[mode] || mode
}

export function renderLiveImageResult(result: TurkuLiveImageResultMetadata): string {
  const review = result.humanReviewRequired ? ' [Inceleme gerekli]' : ''
  return `Sahne ${result.sceneNo}: ${result.artifactLabel} [${result.formatHint} ${result.widthHint}x${result.heightHint}]${review}`
}

export function getLiveGenSummary(result: TurkuLiveGenerationResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderLiveGenStatus(result.status)}`)
  lines.push(`Mod: ${renderLiveGenMode(result.mode)}`)
  lines.push(`Kill Switch: ${result.killSwitchActive ? 'AKTIF' : 'Devre disi'}`)
  lines.push(`Operator Kapisi: ${result.operatorGatePassed ? 'Gecildi' : 'Bekleniyor'}`)
  lines.push(`Saglayici: ${result.providerReady ? 'Hazir' : 'Hazir degil'}`)
  lines.push(`Sonuclar: ${result.generatedCount}/${result.totalScenes}`)
  lines.push(`Dogrulama: ${result.validationSummary}`)
  return lines.join('\n')
}

export function isRealProviderResult(result: TurkuLiveGenerationResult): boolean {
  return result.mode === 'LOCAL_MANUAL_REAL_PROVIDER' && result.status === 'REAL_PROVIDER_RESULT_AVAILABLE'
}

export function areAllGatesPassed(result: TurkuLiveGenerationResult): boolean {
  return !result.killSwitchActive && result.operatorGatePassed && result.providerReady
}
