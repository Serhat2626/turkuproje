/**
 * RP-003: Scene Asset Plan Preview — admin UI.
 * Visual brief cards with safe metadata. No provider prompts.
 */

import type {
  TurkuSceneAssetPlanEntry,
  TurkuSceneAssetPlanResult,
  TurkuSceneAssetPlanStatus,
} from '../../types'

const STATUS_LABELS: Record<TurkuSceneAssetPlanStatus, string> = {
  CREATED_INTERNAL_ONLY: '\u0130\u00e7 Kullan\u0131m \u0130\u00e7in Olu\u015fturuldu',
  DRAFT_ACCEPTED: 'Taslak Kabul Edildi',
  VISUAL_BRIEF_GENERATED: 'G\u00f6rsel \u00d6zet Olu\u015fturuldu',
  READY_FOR_ASSET_GENERATION_BOUNDARY: 'Varl\u0131k Olu\u015fturmaya Haz\u0131r',
  BLOCKED_CONTENT_UNSAFE: 'Engellendi (G\u00fcvensiz \u0130\u00e7erik)',
  REJECTED_UNSAFE: 'Reddedildi (G\u00fcvensiz)',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

export function renderAssetPlanStatus(status: TurkuSceneAssetPlanStatus): string {
  return STATUS_LABELS[status] || status
}

export function isAssetPlanReady(result: TurkuSceneAssetPlanResult): boolean {
  return result.overallStatus === 'READY_FOR_ASSET_GENERATION_BOUNDARY'
}

export function renderAssetPlanCard(entry: TurkuSceneAssetPlanEntry): string {
  const lines: string[] = [`Sahne ${entry.sceneNumber}`]
  lines.push(`Durum: ${renderAssetPlanStatus(entry.status)}`)
  if (entry.visualBriefSummary) lines.push(`G\u00f6rsel \u00d6zet: ${entry.visualBriefSummary}`)
  if (entry.shotType) lines.push(`\u00c7ekim: ${entry.shotType}`)
  if (entry.framing) lines.push(`Kadraj: ${entry.framing}`)
  if (entry.atmosphere) lines.push(`Atmosfer: ${entry.atmosphere}`)
  if (entry.historicalCulturalStyle) lines.push(`Stil: ${entry.historicalCulturalStyle}`)
  if (entry.safetyClassification) lines.push(`G\u00fcvenlik: ${entry.safetyClassification}`)
  if (entry.blockedReason) lines.push(`Engel Nedeni: ${entry.blockedReason}`)
  if (entry.issueCodes.length > 0) lines.push(`Sorunlar: ${entry.issueCodes.join(', ')}`)
  return lines.join('\n')
}

export function getAssetPlanSummary(result: TurkuSceneAssetPlanResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderAssetPlanStatus(result.overallStatus)}`)
  if (result.safeTurkuTitleLabel) lines.push(`T\u00fcrk\u00fc: ${result.safeTurkuTitleLabel}`)
  if (result.plannerMode) lines.push(`Planlay\u0131c\u0131: ${result.plannerMode}`)
  lines.push(`Toplam Sahne: ${result.totalPlannedScenes}`)
  lines.push(`Engellenen: ${result.blockedSceneCount}`)
  if (result.overallIssueCodes.length > 0) lines.push(`Sorunlar: ${result.overallIssueCodes.join(', ')}`)
  return lines.join('\n')
}
