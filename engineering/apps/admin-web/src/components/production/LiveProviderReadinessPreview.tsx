/**
 * LIVE-001: Provider Readiness Preview — admin UI.
 * Credential presence status only. Kill switch visible.
 */

import type {
  TurkuLiveProviderReadinessReport,
  TurkuLiveProviderStatus,
  TurkuKillSwitch,
} from '../../types'

const STATUS_LABELS: Record<TurkuLiveProviderStatus, string> = {
  DISABLED_BY_DEFAULT: 'Varsayilan Devre Disi',
  CREDENTIAL_PRESENT: 'Kimlik Bilgisi Mevcut',
  BLOCKED_BY_KILL_SWITCH: 'Kill Switch Engeli',
  WAITING_FOR_OPERATOR_ENABLEMENT: 'Operator Etkinlestirmesi Bekleniyor',
  FAILED_SAFE: 'Basarisiz (Guvenli)',
}

export function renderLiveProviderStatus(status: TurkuLiveProviderStatus): string {
  return STATUS_LABELS[status] || status
}

export function renderKillSwitch(ks: TurkuKillSwitch): string {
  const icon = ks.active ? '\u274c' : '\u2705'
  return `${icon} Kill Switch: ${ks.active ? 'AKTIF' : 'DEVRE DISI'} — ${ks.reason}`
}

export function getLiveProviderSummary(report: TurkuLiveProviderReadinessReport): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderLiveProviderStatus(report.status)}`)
  lines.push(`Kimlik: ${report.credentialSourceLabel}`)
  lines.push(`Operator: ${report.operatorEnabled ? 'Etkin' : 'Devre disi'}`)
  lines.push(renderKillSwitch(report.killSwitch))
  lines.push(`Dogrulama: ${report.validationSummary}`)
  return lines.join('\n')
}

export function isProviderReady(report: TurkuLiveProviderReadinessReport): boolean {
  return report.status === 'CREDENTIAL_PRESENT' && !report.killSwitch.active && report.operatorEnabled
}
