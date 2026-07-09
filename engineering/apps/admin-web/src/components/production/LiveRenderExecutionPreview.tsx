/**
 * LIVE-004: Render Execution Preview — admin UI.
 * No command/path/stdout/stderr. Opaque refs only.
 */

import type {
  TurkuLiveRenderResult,
  TurkuLiveRenderStatus,
  TurkuLiveRenderMode,
  TurkuLiveRenderArtifact,
} from '../../types'

const STATUS_LABELS: Record<TurkuLiveRenderStatus, string> = {
  NOT_STARTED: 'Baslamadi',
  FAKE_RENDER_AVAILABLE: 'Sahte Render Mevcut',
  REAL_RENDER_AVAILABLE_INTERNAL: 'Gercek Render Mevcut (Dahili)',
  BLOCKED_BY_KILL_SWITCH: 'Kill Switch Engeli',
  BLOCKED_BY_OPERATOR_GATE: 'Operator Kapisi Bekleniyor',
  BLOCKED_BY_ARTIFACT_READINESS: 'Gorsel Dogrulama Bekleniyor',
  FAILED_SAFE: 'Basarisiz (Guvenli)',
}

const MODE_LABELS: Record<TurkuLiveRenderMode, string> = {
  CI_FAKE: 'CI Sahte Mod',
  LOCAL_MANUAL_REAL_FFMPEG: 'Yerel/Manuel Gercek FFmpeg',
}

export function renderRenderStatus(status: TurkuLiveRenderStatus): string {
  return STATUS_LABELS[status] || status
}

export function renderRenderMode(mode: TurkuLiveRenderMode): string {
  return MODE_LABELS[mode] || mode
}

export function renderRenderArtifact(artifact: TurkuLiveRenderArtifact): string {
  const icon = artifact.verified ? '\u2705' : '\u274c'
  const review = artifact.humanReviewRequired ? ' [Inceleme gerekli]' : ''
  return `${icon} ${artifact.artifactLabel} [${artifact.formatHint} ${artifact.resolutionHint} ~${artifact.durationSecondsHint}s]${review}`
}

export function getRenderSummary(result: TurkuLiveRenderResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderRenderStatus(result.status)}`)
  lines.push(`Mod: ${renderRenderMode(result.mode)}`)
  lines.push(`Kill Switch: ${result.killSwitchActive ? 'AKTIF' : 'Devre disi'}`)
  lines.push(`Operator Kapisi: ${result.operatorGatePassed ? 'Gecildi' : 'Bekleniyor'}`)
  lines.push(`Gorseller: ${result.artifactsReady ? 'Hazir' : 'Hazir degil'}`)
  lines.push(`Girdi Sahneleri: ${result.inputSceneCount}`)
  lines.push(`Dogrulama: ${result.validationSummary}`)
  return lines.join('\n')
}

export function isRealRender(result: TurkuLiveRenderResult): boolean {
  return result.mode === 'LOCAL_MANUAL_REAL_FFMPEG' && result.status === 'REAL_RENDER_AVAILABLE_INTERNAL'
}

export function areRenderGatesPassed(result: TurkuLiveRenderResult): boolean {
  return !result.killSwitchActive && result.operatorGatePassed && result.artifactsReady
}
