/**
 * RP-002: Safe Storyboard Draft Preview — admin UI.
 * Scene cards with safe metadata. No lyrics/prompt/script/provider.
 */

import type {
  TurkuStoryboardDraftResult,
  TurkuStoryboardDraftStatus,
  TurkuStoryboardDraftScene,
} from '../../types'

const STATUS_LABELS: Record<TurkuStoryboardDraftStatus, string> = {
  CREATED_INTERNAL_ONLY: '\u0130\u00e7 Kullan\u0131m \u0130\u00e7in Olu\u015fturuldu',
  REQUEST_ACCEPTED: '\u0130stek Kabul Edildi',
  DRAFT_ORCHESTRATION_STARTED: 'Taslak Orkestrasyon Ba\u015flat\u0131ld\u0131',
  SCENES_GENERATED_SAFE: 'Sahneler G\u00fcvenli Olu\u015fturuldu',
  READY_FOR_ADMIN_PREVIEW: 'Admin \u00d6nizlemesine Haz\u0131r',
  REJECTED_UNSAFE: 'Reddedildi (G\u00fcvensiz)',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

export function renderDraftStatus(status: TurkuStoryboardDraftStatus): string {
  return STATUS_LABELS[status] || status
}

export function isDraftReady(result: TurkuStoryboardDraftResult): boolean {
  return result.status === 'READY_FOR_ADMIN_PREVIEW'
}

export function renderSceneCard(scene: TurkuStoryboardDraftScene): string {
  return [
    `Sahne ${scene.sceneNumber}`,
    `Ama\u00e7: ${scene.purpose}`,
    `Ruh Hali: ${scene.mood}`,
    `G\u00f6rsel Y\u00f6n: ${scene.visualDirection}`,
    `S\u00fcre: ${scene.durationMs}ms`,
  ].join('\n')
}

export function getDraftSummary(result: TurkuStoryboardDraftResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderDraftStatus(result.status)}`)
  if (result.safeTurkuTitleLabel) {
    lines.push(`T\u00fcrk\u00fc: ${result.safeTurkuTitleLabel}`)
  }
  if (result.orchestrationMode) {
    lines.push(`Orkestrasyon: ${result.orchestrationMode}`)
  }
  lines.push(`Sahne Say\u0131s\u0131: ${result.totalSceneCount}`)
  lines.push(`Toplam S\u00fcre: ${result.totalDurationMs}ms`)
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
