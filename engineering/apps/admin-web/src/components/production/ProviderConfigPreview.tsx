/**
 * RPE-002: Provider Configuration Preview — admin UI.
 * All provider execution disabled. Metadata only.
 */

import type {
  TurkuProviderConfigResult,
  TurkuProviderConfigStatus,
  TurkuCredentialVaultBoundary,
} from '../../types'

const STATUS_LABELS: Record<TurkuProviderConfigStatus, string> = {
  NOT_CONFIGURED: 'Yap\u0131land\u0131r\u0131lmad\u0131',
  CONFIGURATION_DRAFT: 'Yap\u0131land\u0131rma Tasla\u011f\u0131',
  BLOCKED_BY_SAFETY: 'G\u00fcvenlik Engeli',
  WAITING_FOR_OPERATOR_REVIEW: 'Operat\u00f6r \u0130ncelemesi Bekleniyor',
  FAILED_SAFE: 'Ba\u015far\u0131s\u0131z (G\u00fcvenli)',
}

export function renderConfigStatus(status: TurkuProviderConfigStatus): string {
  return STATUS_LABELS[status] || status
}

export function isProviderExecutionEnabled(result: TurkuProviderConfigResult): boolean {
  return result.executionEnabled
}

export function getConfigSummary(result: TurkuProviderConfigResult): string {
  const lines: string[] = []
  lines.push(`Durum: ${renderConfigStatus(result.status)}`)
  if (result.providerLabel) lines.push(`Sa\u011flay\u0131c\u0131: ${result.providerLabel}`)
  lines.push(`G\u00fcvenlik: ${result.safetyValidationSummary}`)
  return lines.join('\n')
}

export function renderVaultBoundary(vault: TurkuCredentialVaultBoundary | null): string {
  if (!vault) return 'Kasa bilgisi yok'
  const icon = vault.credentialPresent ? '\u2705' : '\u274c'
  return `${icon} ${vault.vaultLabel}: ${vault.presenceStatusLabel}`
}

export function getCapabilityLabels(result: TurkuProviderConfigResult): string {
  return result.capabilityLabels.map((l, i) => `${i + 1}. ${l}`).join('\n')
}

export function getSelectionPolicy(result: TurkuProviderConfigResult): string {
  return result.selectionPolicySummary || 'Belirlenmedi'
}

export function getFallbackPolicy(result: TurkuProviderConfigResult): string {
  return result.fallbackPolicySummary || 'Belirlenmedi'
}
