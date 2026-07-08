/**
 * RPE-001: Execution Safety Gate Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderGateStatus,
  isExecutionEnabled,
  renderCapability,
  getGateSummary,
  getOperatorGates,
  getFutureSteps,
  getGateBlockedReasons,
} from '../ExecutionGatePreview'
import type { TurkuExecutionGateResult, TurkuExecutionCapability } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey', 'storageKey', 'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal',
  'lyrics', 'scriptBody', 'promptText', 'providerPrompt',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'GenerateButton', 'ImageGenerator', 'ExecuteButton',
  'innerHTML', 'dangerouslySetInnerHTML',
]

function makeCap(enabled: boolean): TurkuExecutionCapability {
  return { capabilityLabel: 'G\u00f6rsel \u00fcretim', enabled, disabledReason: enabled ? '' : 'RPE-001: devre d\u0131\u015f\u0131' }
}

function makeNotEnabled(): TurkuExecutionGateResult {
  return {
    status: 'EXECUTION_NOT_ENABLED',
    blockedReasons: ['IMAGE_PROVIDER_NOT_CONFIGURED', 'OPERATOR_GATE_NOT_PASSED'],
    opaqueExecutionRef: 'exec:abc', opaqueWorkOrderRef: 'wo:def',
    safeTurkuTitleLabel: '\u00c7anakkale T\u00fcrk\u00fcs\u00fc',
    auditDecisionSummary: 'Y\u00fcr\u00fctme hen\u00fcz etkinle\u015ftirilmedi',
    capabilities: [makeCap(false), makeCap(false)],
    requiredOperatorGates: ['Sa\u011flay\u0131c\u0131 onay\u0131', 'Render onay\u0131'],
    futureExecutionStepLabels: ['RPE-002 sa\u011flay\u0131c\u0131 entegrasyonu', 'RPE-003 render'],
  }
}

function makeBlocked(): TurkuExecutionGateResult {
  return {
    ...makeNotEnabled(), status: 'BLOCKED_BY_SAFETY',
    blockedReasons: ['WORK_ORDER_MISSING'],
    auditDecisionSummary: '\u0130\u015f emri eksik',
  }
}

describe('RPE-001 \u2014 Execution Gate Preview', () => {
  describe('A. Status Rendering', () => {
    it('not enabled', () => expect(renderGateStatus('EXECUTION_NOT_ENABLED')).toContain('Etkinle\u015ftirilmedi'))
    it('blocked', () => expect(renderGateStatus('BLOCKED_BY_SAFETY')).toContain('Engel'))
    it('waiting', () => expect(renderGateStatus('WAITING_FOR_OPERATOR_GATE')).toContain('Bekleniyor'))
    it('eligible', () => expect(renderGateStatus('ELIGIBLE_FOR_FUTURE_EXECUTION_REVIEW')).toContain('Uygun'))
  })

  describe('B. Execution Disabled', () => {
    it('not enabled = no capabilities enabled', () => expect(isExecutionEnabled(makeNotEnabled())).toBe(false))
    it('blocked = no capabilities enabled', () => expect(isExecutionEnabled(makeBlocked())).toBe(false))
  })

  describe('C. Capability Rendering', () => {
    it('disabled shows red', () => expect(renderCapability(makeCap(false))).toContain('\u274c'))
    it('enabled shows green', () => expect(renderCapability(makeCap(true))).toContain('\u2705'))
    it('disabled shows reason', () => expect(renderCapability(makeCap(false))).toContain('RPE-001'))
  })

  describe('D. Summary', () => {
    it('not enabled summary', () => expect(getGateSummary(makeNotEnabled())).toContain('etkinle\u015ftirilmedi'))
    it('blocked summary', () => expect(getGateSummary(makeBlocked())).toContain('eksik'))
  })

  describe('E. Operator Gates', () => {
    it('gates rendered', () => expect(getOperatorGates(makeNotEnabled())).toContain('onay'))
  })

  describe('F. Future Steps', () => {
    it('steps rendered', () => expect(getFutureSteps(makeNotEnabled())).toContain('RPE-002'))
  })

  describe('G. Blocked Reasons', () => {
    it('not enabled shows codes', () => expect(getGateBlockedReasons(makeNotEnabled())).toContain('IMAGE_PROVIDER'))
    it('blocked shows codes', () => expect(getGateBlockedReasons(makeBlocked())).toContain('WORK_ORDER_MISSING'))
  })

  describe('H. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(getGateSummary(makeNotEnabled())).not.toContain(f)
    })
    it('capability no forbidden', () => {
      for (const f of FORBIDDEN_TEXT) expect(renderCapability(makeCap(false))).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/ExecutionGatePreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('I. T\u00fcrk\u00e7e Characters', () => {
    it('status', () => expect(renderGateStatus('FAILED_SAFE')).toContain('Ba\u015far\u0131s\u0131z'))
    it('capability', () => expect(renderCapability(makeCap(false))).toContain('\u00fcretim'))
  })
})
