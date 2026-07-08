/**
 * RP-004: Animatic Timeline Preview — admin UI.
 * Timeline items with safe metadata. No render or real processing.
 */

import type {
  TurkuAnimaticTimelineItem,
  TurkuAnimaticTimelinePlanResult,
  TurkuAnimaticTimelinePlanStatus,
} from '../../types'

const STATUS_LABELS: Record<TurkuAnimaticTimelinePlanStatus, string> = {
  CREATED_INTERNAL_ONLY: '\u0130\u00e7 Kullan\u0131m \u0130\u00e7in Olu\u015fturuldu',
  ASSET_PLAN_ACCEPTED: 'Varl\u0131k Plan\u0131 Kabul Edildi',
  TIMELINE_ITEMS_GENERATED: 'Zaman \u00c7izelgesi Olu\u015fturuldu',
  READY_FOR_ASSEMBLY_BOUNDARY: 'Montaja Haz\u0131r',
  REJECTED_UNSAFE: 'Reddedildi (G\u00fcvensiz)',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

export function renderTimelineStatus(status: TurkuAnimaticTimelinePlanStatus): string {
  return STATUS_LABELS[status] || status
}

export function isTimelineReady(result: TurkuAnimaticTimelinePlanResult): boolean {
  return result.status === 'READY_FOR_ASSEMBLY_BOUNDARY'
}

export function renderTimelineItem(item: TurkuAnimaticTimelineItem): string {
  return [
    `Sahne ${item.sceneNumber}`,
    `Ba\u015flang\u0131\u00e7: ${item.startMs}ms`,
    `S\u00fcre: ${item.durationMs}ms`,
    `Ge\u00e7i\u015f: ${item.transitionType}`,
    `Kamera: ${item.cameraMovement}`,
    `Tempo: ${item.visualPacingSummary}`,
    `Anlat\u0131: ${item.narrativeBeatSummary}`,
  ].join('\n')
}

export function getTimelineSummary(result: TurkuAnimaticTimelinePlanResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderTimelineStatus(result.status)}`)
  if (result.safeTurkuTitleLabel) lines.push(`T\u00fcrk\u00fc: ${result.safeTurkuTitleLabel}`)
  if (result.plannerMode) lines.push(`Planlay\u0131c\u0131: ${result.plannerMode}`)
  lines.push(`\u00d6\u011fe Say\u0131s\u0131: ${result.totalItems}`)
  lines.push(`Toplam S\u00fcre: ${result.totalDurationMs}ms`)
  if (result.issueCodes.length > 0) lines.push(`Sorunlar: ${result.issueCodes.join(', ')}`)
  return lines.join('\n')
}
