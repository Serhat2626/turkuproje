/**
 * MP-002: Work Order Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderWorkOrderStatus,
  isEligibleForManualPreparation,
  renderChecklistItem,
  getWorkOrderSummary,
  getWorkOrderSteps,
  getWorkOrderBlockedReasons,
} from '../WorkOrderPreview'
import type { TurkuWorkOrderPreviewResult, TurkuProductionChecklistItem } from '../../../types'

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

function makeItem(met: boolean): TurkuProductionChecklistItem {
  return {
    order: 1, label: 'Sa\u011flay\u0131c\u0131 yap\u0131land\u0131rmas\u0131',
    instruction: 'Manuel olarak yap\u0131land\u0131r\u0131n',
    prerequisiteMet: met, blockedReason: met ? null : '\u00d6n ko\u015ful eksik',
  }
}

function makeEligible(): TurkuWorkOrderPreviewResult {
  return {
    status: 'ELIGIBLE_FOR_MANUAL_PREPARATION', blockedReasons: [],
    opaqueWorkOrderRef: 'wo:abc', opaqueEnablementRef: 'en:def',
    safeTurkuTitleLabel: '\u00c7anakkale T\u00fcrk\u00fcs\u00fc',
    operatorSummary: '\u00c7anakkale \u2014 6/6 kontrol tamamland\u0131',
    checklistItems: [makeItem(true), makeItem(true)],
    totalItems: 2, completedItems: 2, blockedItems: 0,
    workOrderStepLabels: ['Kontrol listesini tamamlay\u0131n', 'Render ortam\u0131n\u0131 haz\u0131rlay\u0131n'],
  }
}

function makeBlocked(): TurkuWorkOrderPreviewResult {
  return {
    ...makeEligible(), status: 'BLOCKED_BY_SAFETY',
    blockedReasons: ['ENABLEMENT_NOT_ELIGIBLE'],
    operatorSummary: 'Uygun de\u011fil',
    checklistItems: [makeItem(false)],
    totalItems: 1, completedItems: 0, blockedItems: 1,
    workOrderStepLabels: ['Engelleri \u00e7\u00f6z\u00fcn'],
  }
}

function makeWaiting(): TurkuWorkOrderPreviewResult {
  return {
    ...makeEligible(), status: 'WAITING_FOR_MANUAL_REVIEW',
    blockedReasons: ['CHECKLIST_INCOMPLETE'],
    completedItems: 1, blockedItems: 1,
  }
}

describe('MP-002 \u2014 Work Order Preview', () => {
  describe('A. Status Rendering', () => {
    it('eligible renders Uygun', () => {
      expect(renderWorkOrderStatus('ELIGIBLE_FOR_MANUAL_PREPARATION')).toContain('Uygun')
    })
    it('blocked renders Engel', () => {
      expect(renderWorkOrderStatus('BLOCKED_BY_SAFETY')).toContain('Engel')
    })
    it('waiting renders Bekleniyor', () => {
      expect(renderWorkOrderStatus('WAITING_FOR_MANUAL_REVIEW')).toContain('Bekleniyor')
    })
    it('draft renders \u00d6nizleme', () => {
      expect(renderWorkOrderStatus('DRAFT_PREVIEW')).toContain('\u00d6nizleme')
    })
  })

  describe('B. Eligibility Check', () => {
    it('eligible true', () => expect(isEligibleForManualPreparation(makeEligible())).toBe(true))
    it('blocked false', () => expect(isEligibleForManualPreparation(makeBlocked())).toBe(false))
    it('waiting false', () => expect(isEligibleForManualPreparation(makeWaiting())).toBe(false))
  })

  describe('C. Checklist Item Rendering', () => {
    it('met item shows green', () => {
      expect(renderChecklistItem(makeItem(true))).toContain('\u2705')
    })
    it('not met item shows red', () => {
      expect(renderChecklistItem(makeItem(false))).toContain('\u274c')
    })
    it('not met shows blocked reason', () => {
      expect(renderChecklistItem(makeItem(false))).toContain('Engel')
    })
  })

  describe('D. Summary', () => {
    it('eligible summary shows tamamland\u0131', () => {
      expect(getWorkOrderSummary(makeEligible())).toContain('tamamland')
    })
    it('blocked summary shows Engel', () => {
      expect(getWorkOrderSummary(makeBlocked())).toContain('Engel')
    })
  })

  describe('E. Work Order Steps', () => {
    it('eligible has steps', () => {
      expect(getWorkOrderSteps(makeEligible())).toContain('tamamlay')
    })
    it('blocked has fix steps', () => {
      expect(getWorkOrderSteps(makeBlocked())).toContain('\u00e7\u00f6z\u00fcn')
    })
  })

  describe('F. Blocked Reasons', () => {
    it('eligible no blocks', () => {
      expect(getWorkOrderBlockedReasons(makeEligible())).toContain('yok')
    })
    it('blocked shows codes', () => {
      expect(getWorkOrderBlockedReasons(makeBlocked())).toContain('ENABLEMENT_NOT_ELIGIBLE')
    })
  })

  describe('G. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      const s = getWorkOrderSummary(makeEligible())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('checklist item no forbidden', () => {
      const s = renderChecklistItem(makeItem(true))
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/WorkOrderPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('H. T\u00fcrk\u00e7e Characters', () => {
    it('status labels', () => {
      expect(renderWorkOrderStatus('FAILED_SAFE')).toContain('Ba\u015far\u0131s\u0131z')
    })
    it('checklist labels', () => {
      expect(renderChecklistItem(makeItem(true))).toContain('Sa\u011flay\u0131c\u0131')
    })
  })
})
