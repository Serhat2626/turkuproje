/**
 * RPE-004: Execution Package Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderPackageStatus,
  isAnyCapabilityEnabled,
  renderManualGate,
  renderCapabilityFlag,
  getPackageSummary,
  getFutureStepLabels,
  getBlockedReasons,
} from '../ExecutionPackagePreview'
import type { TurkuExecutionPackageResult, TurkuManualGate, TurkuExecutionCapability } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'storageKey', 'base64',
  'token', 'JWT', 'session', 'principal', 'apiKey', 'credential',
  'lyrics', 'scriptBody', 'promptText', 'providerPrompt', 'rawPrompt',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'GenerateButton', 'ExecuteButton', 'FfmpegButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeGate(passed: boolean): TurkuManualGate {
  return { order: 1, gateLabel: 'Test kapisi', instruction: 'Talimat', passed, blockedReason: passed ? null : 'Bekleniyor' }
}

function makeCap(enabled: boolean): TurkuExecutionCapability {
  return { capabilityLabel: 'Gorsel uretim', enabled, disabledReason: enabled ? '' : 'RPE-004: devre disi' }
}

function makeWaiting(): TurkuExecutionPackageResult {
  return {
    status: 'WAITING_FOR_MANUAL_GATE',
    blockedReasons: ['MANUAL_GATE_NOT_PASSED', 'IMAGE_PROVIDER_NOT_CONFIGURED'],
    opaquePackageRef: 'epkg:abc', opaqueWorkOrderRef: 'wo:def',
    safeTurkuTitleLabel: 'Canakkale Turkusu',
    providerConfigSummary: 'Saglayici: NOT_CONFIGURED',
    renderConfigSummary: 'Render: NOT_CONFIGURED',
    manualGates: [makeGate(false), makeGate(false)],
    executionCapabilities: [makeCap(false), makeCap(false)],
    operatorExecutionSummary: 'Yurutme paketi hazir degil',
    futureExecutionStepLabels: ['RPE-005 saglayici', 'RPE-006 render'],
    validationSummary: 'Dogrulama: 3 sorun',
  }
}

function makeBlocked(): TurkuExecutionPackageResult {
  return { ...makeWaiting(), status: 'BLOCKED_BY_SAFETY', blockedReasons: ['WORK_ORDER_NOT_ELIGIBLE'] }
}

describe('RPE-004 — Execution Package Preview', () => {
  describe('A. Status Rendering', () => {
    it('waiting', () => expect(renderPackageStatus('WAITING_FOR_MANUAL_GATE')).toContain('Bekleniyor'))
    it('blocked', () => expect(renderPackageStatus('BLOCKED_BY_SAFETY')).toContain('Engel'))
    it('not ready', () => expect(renderPackageStatus('NOT_READY')).toContain('Degil'))
    it('draft', () => expect(renderPackageStatus('PACKAGE_DRAFT')).toContain('Taslagi'))
  })

  describe('B. Capabilities Disabled', () => {
    it('waiting = none enabled', () => expect(isAnyCapabilityEnabled(makeWaiting())).toBe(false))
    it('blocked = none enabled', () => expect(isAnyCapabilityEnabled(makeBlocked())).toBe(false))
  })

  describe('C. Manual Gates', () => {
    it('not passed shows red', () => expect(renderManualGate(makeGate(false))).toContain('\u274c'))
    it('passed shows green', () => expect(renderManualGate(makeGate(true))).toContain('\u2705'))
    it('not passed shows blocked', () => expect(renderManualGate(makeGate(false))).toContain('Bekleniyor'))
  })

  describe('D. Capability Flags', () => {
    it('disabled shows red', () => expect(renderCapabilityFlag(makeCap(false))).toContain('\u274c'))
    it('disabled shows RPE-004', () => expect(renderCapabilityFlag(makeCap(false))).toContain('RPE-004'))
  })

  describe('E. Summary', () => {
    it('includes status', () => expect(getPackageSummary(makeWaiting())).toContain('Bekleniyor'))
    it('includes provider', () => expect(getPackageSummary(makeWaiting())).toContain('NOT_CONFIGURED'))
    it('includes render', () => expect(getPackageSummary(makeWaiting())).toContain('Render'))
  })

  describe('F. Future Steps', () => {
    it('listed', () => expect(getFutureStepLabels(makeWaiting())).toContain('RPE-005'))
  })

  describe('G. Blocked Reasons', () => {
    it('shows codes', () => expect(getBlockedReasons(makeWaiting())).toContain('MANUAL_GATE'))
    it('empty', () => expect(getBlockedReasons({ ...makeWaiting(), blockedReasons: [] })).toContain('yok'))
  })

  describe('H. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getPackageSummary(makeWaiting())).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/ExecutionPackagePreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('I. Type Safety', () => {
    it('no forbidden keys', () => {
      for (const f of ['token', 'secret', 'filePath', 'commandLine', 'stdout', 'lyrics'])
        expect(Object.keys(makeWaiting())).not.toContain(f)
    })
  })
})
