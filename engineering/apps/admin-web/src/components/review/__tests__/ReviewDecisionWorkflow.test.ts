/**
 * PD-005: Human Review Decision Workflow UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderDecisionStatus,
  renderDecisionLabel,
  isDecisionApproved,
  getDecisionSummary,
  getDecisionControls,
} from '../ReviewDecisionWorkflow'
import type { ProductionMediaReviewDecisionResult } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'inputPath', 'outputPath', 'workspacePath',
  'stdout', 'stderr', 'commandLine', 'rawLog', 'shellCommand', 'executablePath',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey', 'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal', 'authorizationHeader',
  'lyrics', 'scriptBody', 'promptText', 'copyrightedContent', 'userMedia',
]

const PANEL_FORBIDDEN = [
  'PublishButton', 'DownloadButton', 'ExportButton',
  'VideoPlayer', 'StreamPlayer', 'innerHTML', 'dangerouslySetInnerHTML',
  'publicUrl', 'signedUrl', 'cdnUrl',
]

function makeApproved(): ProductionMediaReviewDecisionResult {
  return {
    status: 'APPROVED_FOR_INTERNAL_DELIVERY_PREP',
    decisionType: 'APPROVE_FOR_INTERNAL_DELIVERY_PREP',
    issueCodes: [],
    opaqueDecisionRef: 'decision:abc',
    internalOnly: true,
  }
}

function makeRejected(): ProductionMediaReviewDecisionResult {
  return {
    status: 'REJECTED_FOR_INTERNAL_REVISION',
    decisionType: 'REJECT_FOR_INTERNAL_REVISION',
    issueCodes: ['PREVIEW_NOT_ELIGIBLE'],
    opaqueDecisionRef: null,
    internalOnly: true,
  }
}

function makeFailed(): ProductionMediaReviewDecisionResult {
  return {
    status: 'FAILED_SAFE',
    decisionType: null,
    issueCodes: ['INTERNAL_ERROR'],
    opaqueDecisionRef: null,
    internalOnly: true,
  }
}

describe('PD-005 \u2014 Human Review Decision Workflow', () => {
  describe('A. Status Rendering', () => {
    it('approved renders Onayland\u0131', () => {
      expect(renderDecisionStatus('APPROVED_FOR_INTERNAL_DELIVERY_PREP')).toContain('Onayland\u0131')
    })
    it('rejected renders Reddedildi', () => {
      expect(renderDecisionStatus('REJECTED_FOR_INTERNAL_REVISION')).toContain('Reddedildi')
    })
    it('fix requested renders Talep', () => {
      expect(renderDecisionStatus('INTERNAL_FIX_REQUESTED')).toContain('Talep')
    })
    it('failed renders Ba\u015far\u0131s\u0131z', () => {
      expect(renderDecisionStatus('FAILED_SAFE')).toContain('Ba\u015far\u0131s\u0131z')
    })
  })

  describe('B. Decision Labels', () => {
    it('approve label has Onayla', () => {
      expect(renderDecisionLabel('APPROVE_FOR_INTERNAL_DELIVERY_PREP')).toContain('Onayla')
    })
    it('reject label has Reddet', () => {
      expect(renderDecisionLabel('REJECT_FOR_INTERNAL_REVISION')).toContain('Reddet')
    })
    it('fix label has D\u00fczeltme', () => {
      expect(renderDecisionLabel('REQUEST_INTERNAL_FIX')).toContain('D\u00fczeltme')
    })
    it('unsafe label has \u0130\u015faretle', () => {
      expect(renderDecisionLabel('MARK_UNSAFE_INTERNAL_ONLY')).toContain('\u0130\u015faretle')
    })
  })

  describe('C. Approval Check', () => {
    it('approved returns true', () => expect(isDecisionApproved(makeApproved())).toBe(true))
    it('rejected returns false', () => expect(isDecisionApproved(makeRejected())).toBe(false))
    it('failed returns false', () => expect(isDecisionApproved(makeFailed())).toBe(false))
  })

  describe('D. Decision Summary', () => {
    it('approved summary', () => {
      const s = getDecisionSummary(makeApproved())
      expect(s).toContain('Onayland\u0131')
      expect(s).toContain('Dahili: Evet')
    })
    it('rejected summary with issue codes', () => {
      const s = getDecisionSummary(makeRejected())
      expect(s).toContain('PREVIEW_NOT_ELIGIBLE')
    })
  })

  describe('E. Decision Controls', () => {
    it('returns 4 controls', () => {
      expect(getDecisionControls()).toHaveLength(4)
    })
    it('no publish control', () => {
      const labels = getDecisionControls().map(c => c.label)
      for (const l of labels) {
        expect(l).not.toContain('Publish')
        expect(l).not.toContain('Download')
        expect(l).not.toContain('Export')
      }
    })
    it('all controls internal', () => {
      for (const c of getDecisionControls()) {
        expect(c.type).toContain('INTERNAL')
      }
    })
  })

  describe('F. Forbidden Text Sweep', () => {
    it('approved summary no forbidden', () => {
      const s = getDecisionSummary(makeApproved())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('rejected summary no forbidden', () => {
      const s = getDecisionSummary(makeRejected())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
  })

  describe('G. Source File Safety', () => {
    it('component has no delivery/unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/review/ReviewDecisionWorkflow.tsx', 'utf-8')
      for (const f of PANEL_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('H. T\u00fcrk\u00e7e Characters', () => {
    it('status labels', () => {
      expect(renderDecisionStatus('CREATED_INTERNAL_ONLY')).toContain('Olu\u015fturuldu')
    })
    it('decision labels', () => {
      expect(renderDecisionLabel('REQUEST_INTERNAL_FIX')).toContain('D\u00fczeltme')
    })
  })
})
