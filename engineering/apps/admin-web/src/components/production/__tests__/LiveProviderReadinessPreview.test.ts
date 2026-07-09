/**
 * LIVE-001: Provider Readiness Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderLiveProviderStatus,
  renderKillSwitch,
  getLiveProviderSummary,
  isProviderReady,
} from '../LiveProviderReadinessPreview'
import type { TurkuLiveProviderReadinessReport, TurkuKillSwitch } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'storageKey', 'base64',
  'token', 'JWT', 'session', 'principal', 'apiKey', 'credential',
  'lyrics', 'scriptBody', 'providerPrompt', 'rawPrompt', 'providerPayload',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'GenerateButton', 'ExecuteButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeKs(active: boolean): TurkuKillSwitch {
  return { active, reason: active ? 'Varsayilan engel' : 'Test', activatedBy: active ? 'SYSTEM' : 'OPERATOR' }
}

function makeBlocked(): TurkuLiveProviderReadinessReport {
  return {
    status: 'BLOCKED_BY_KILL_SWITCH', issues: ['KILL_SWITCH_ACTIVE'],
    credentialPresent: true, credentialSourceLabel: 'Mevcut (gizli)',
    operatorEnabled: true, killSwitch: makeKs(true),
    prerequisiteLabels: ['Kimlik bilgisi', 'Operator etkinlestirmesi'],
    blockedReasons: ['Kill switch aktif'], providerTypeLabel: 'OpenAI',
    validationSummary: '1 sorun',
  }
}

function makeReady(): TurkuLiveProviderReadinessReport {
  return {
    ...makeBlocked(), status: 'CREDENTIAL_PRESENT', issues: [],
    killSwitch: makeKs(false), blockedReasons: [],
    validationSummary: '0 sorun, hazir',
  }
}

function makeDisabled(): TurkuLiveProviderReadinessReport {
  return {
    ...makeBlocked(), status: 'DISABLED_BY_DEFAULT', credentialPresent: false,
    credentialSourceLabel: 'Mevcut degil', operatorEnabled: false,
    killSwitch: makeKs(false), issues: ['CREDENTIAL_NOT_PRESENT'],
  }
}

describe('LIVE-001 — Provider Readiness Preview', () => {
  describe('A. Status Rendering', () => {
    it('blocked', () => expect(renderLiveProviderStatus('BLOCKED_BY_KILL_SWITCH')).toContain('Kill'))
    it('disabled', () => expect(renderLiveProviderStatus('DISABLED_BY_DEFAULT')).toContain('Devre'))
    it('credential present', () => expect(renderLiveProviderStatus('CREDENTIAL_PRESENT')).toContain('Mevcut'))
    it('waiting', () => expect(renderLiveProviderStatus('WAITING_FOR_OPERATOR_ENABLEMENT')).toContain('Bekleniyor'))
  })

  describe('B. Kill Switch', () => {
    it('active shows red', () => expect(renderKillSwitch(makeKs(true))).toContain('\u274c'))
    it('inactive shows green', () => expect(renderKillSwitch(makeKs(false))).toContain('\u2705'))
    it('active shows AKTIF', () => expect(renderKillSwitch(makeKs(true))).toContain('AKTIF'))
  })

  describe('C. Provider Ready Check', () => {
    it('blocked = not ready', () => expect(isProviderReady(makeBlocked())).toBe(false))
    it('ready = ready', () => expect(isProviderReady(makeReady())).toBe(true))
    it('disabled = not ready', () => expect(isProviderReady(makeDisabled())).toBe(false))
  })

  describe('D. Summary', () => {
    it('includes status', () => expect(getLiveProviderSummary(makeBlocked())).toContain('Kill'))
    it('includes credential label', () => expect(getLiveProviderSummary(makeBlocked())).toContain('gizli'))
    it('includes kill switch', () => expect(getLiveProviderSummary(makeBlocked())).toContain('AKTIF'))
  })

  describe('E. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getLiveProviderSummary(makeBlocked())).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/LiveProviderReadinessPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('F. Type Safety', () => {
    it('no forbidden keys', () => {
      for (const f of ['token', 'secret', 'apiKey', 'password', 'filePath', 'base64'])
        expect(Object.keys(makeBlocked())).not.toContain(f)
    })
  })
})
