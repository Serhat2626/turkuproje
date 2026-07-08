/**
 * RPE-002: Provider Configuration Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderConfigStatus,
  isProviderExecutionEnabled,
  getConfigSummary,
  renderVaultBoundary,
  getCapabilityLabels,
  getSelectionPolicy,
  getFallbackPolicy,
} from '../ProviderConfigPreview'
import type { TurkuProviderConfigResult, TurkuCredentialVaultBoundary } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'storageKey', 'base64',
  'token', 'JWT', 'session', 'principal', 'apiKey', 'credential', 'password',
  'lyrics', 'scriptBody', 'promptText', 'providerPrompt', 'rawPrompt',
  'providerPayload', 'rawPayload', 'secret', 'envValue', 'configSecret',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'GenerateButton', 'ImageGenerator', 'ExecuteButton',
  'TestConnectionButton', 'SaveCredentialButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeVault(): TurkuCredentialVaultBoundary {
  return { vaultLabel: 'G\u00f6rsel sa\u011flay\u0131c\u0131 kasas\u0131', credentialPresent: false, presenceStatusLabel: 'Mevcut de\u011fil' }
}

function makeNotConfigured(): TurkuProviderConfigResult {
  return {
    status: 'NOT_CONFIGURED',
    issues: ['CREDENTIAL_NOT_PRESENT', 'PROVIDER_TYPE_NOT_SELECTED'],
    opaqueConfigRef: 'pcfg:abc',
    providerLabel: 'Deterministik sahte sa\u011flay\u0131c\u0131',
    providerType: 'DETERMINISTIC_FAKE',
    capabilityLabels: ['Metin-g\u00f6rsel \u00fcretim', 'Stil transferi'],
    vaultBoundary: makeVault(),
    selectionPolicySummary: 'Hen\u00fcz yap\u0131land\u0131r\u0131lmad\u0131',
    fallbackPolicySummary: 'Hen\u00fcz yap\u0131land\u0131r\u0131lmad\u0131',
    executionEnabled: false,
    safetyValidationSummary: 'Devre d\u0131\u015f\u0131',
  }
}

function makeBlocked(): TurkuProviderConfigResult {
  return { ...makeNotConfigured(), status: 'BLOCKED_BY_SAFETY', issues: ['SAFETY_VALIDATION_FAILED'] }
}

describe('RPE-002 \u2014 Provider Config Preview', () => {
  describe('A. Status Rendering', () => {
    it('not configured', () => expect(renderConfigStatus('NOT_CONFIGURED')).toContain('Yap\u0131land\u0131r\u0131lmad\u0131'))
    it('blocked', () => expect(renderConfigStatus('BLOCKED_BY_SAFETY')).toContain('Engel'))
    it('waiting', () => expect(renderConfigStatus('WAITING_FOR_OPERATOR_REVIEW')).toContain('Bekleniyor'))
    it('draft', () => expect(renderConfigStatus('CONFIGURATION_DRAFT')).toContain('Tasl'))
    it('failed', () => expect(renderConfigStatus('FAILED_SAFE')).toContain('venli'))
  })

  describe('B. Execution Disabled', () => {
    it('not configured = disabled', () => expect(isProviderExecutionEnabled(makeNotConfigured())).toBe(false))
    it('blocked = disabled', () => expect(isProviderExecutionEnabled(makeBlocked())).toBe(false))
  })

  describe('C. Vault Boundary', () => {
    it('not present shows red', () => expect(renderVaultBoundary(makeVault())).toContain('\u274c'))
    it('present shows green', () => expect(renderVaultBoundary({ ...makeVault(), credentialPresent: true, presenceStatusLabel: 'Mevcut' })).toContain('\u2705'))
    it('null vault', () => expect(renderVaultBoundary(null)).toContain('yok'))
  })

  describe('D. Summary', () => {
    it('includes status', () => expect(getConfigSummary(makeNotConfigured())).toContain('Yap\u0131land\u0131r\u0131lmad\u0131'))
    it('includes provider', () => expect(getConfigSummary(makeNotConfigured())).toContain('sahte'))
  })

  describe('E. Capabilities', () => {
    it('listed', () => expect(getCapabilityLabels(makeNotConfigured())).toContain('\u00fcretim'))
  })

  describe('F. Selection/Fallback', () => {
    it('selection', () => expect(getSelectionPolicy(makeNotConfigured())).toContain('yap\u0131land\u0131r\u0131lmad\u0131'))
    it('fallback', () => expect(getFallbackPolicy(makeNotConfigured())).toContain('yap\u0131land\u0131r\u0131lmad\u0131'))
    it('null selection', () => expect(getSelectionPolicy({ ...makeNotConfigured(), selectionPolicySummary: null })).toContain('Belirlenmedi'))
    it('null fallback', () => expect(getFallbackPolicy({ ...makeNotConfigured(), fallbackPolicySummary: null })).toContain('Belirlenmedi'))
  })

  describe('G. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getConfigSummary(makeNotConfigured())).not.toContain(f)
    })
    it('vault no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(renderVaultBoundary(makeVault())).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/ProviderConfigPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('H. Type Safety', () => {
    it('no forbidden keys', () => {
      for (const f of ['token', 'secret', 'apiKey', 'credential', 'password', 'envValue'])
        expect(Object.keys(makeNotConfigured())).not.toContain(f)
    })
  })
})
