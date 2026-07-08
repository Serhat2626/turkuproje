/**
 * MP-001: Manual Production Enablement Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderEnablementStatus,
  isEligibleForManualPrep,
  getEnablementSummary,
  getSafetyPrerequisites,
  getPrerequisiteChecklist,
  getBlockedReasons,
  getManualSteps,
} from '../ManualEnablementPreview'
import type { TurkuManualEnablementResult } from '../../../types'

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

function makeEligible(): TurkuManualEnablementResult {
  return {
    status: 'ELIGIBLE_FOR_MANUAL_PREP', blockedReasons: [],
    opaqueReadinessRef: 'req:abc', safeTurkuTitleLabel: '\u00c7anakkale T\u00fcrk\u00fcs\u00fc',
    operatorSummary: '\u00c7anakkale T\u00fcrk\u00fcs\u00fc \u2014 Manuel \u00fcretim haz\u0131rl\u0131\u011f\u0131na uygun',
    safetyPrerequisites: ['Telif g\u00fcvenli niyet', 'T\u00fcm sahneler planl\u0131'],
    manualStepLabels: ['G\u00f6rsel sa\u011flay\u0131c\u0131 yap\u0131land\u0131rmas\u0131', 'Son manuel kontrol'],
    copyrightSafeIntentPresent: true, allScenesPlanned: true,
    timelineComplete: true, noBlockedScenes: true,
  }
}

function makeBlocked(): TurkuManualEnablementResult {
  return {
    ...makeEligible(), status: 'BLOCKED_BY_SAFETY',
    blockedReasons: ['BLOCKED_SCENES_PRESENT'],
    operatorSummary: 'G\u00fcvenlik engeli',
    noBlockedScenes: false,
    manualStepLabels: ['Engellenen sahneleri \u00e7\u00f6z\u00fcn'],
  }
}

function makeNotEligible(): TurkuManualEnablementResult {
  return {
    ...makeEligible(), status: 'NOT_ELIGIBLE',
    blockedReasons: ['READINESS_MISSING'],
    operatorSummary: 'Haz\u0131rl\u0131k paketi eksik',
    copyrightSafeIntentPresent: false, allScenesPlanned: false,
    timelineComplete: false, noBlockedScenes: false,
  }
}

describe('MP-001 \u2014 Manual Production Enablement Preview', () => {
  describe('A. Status Rendering', () => {
    it('eligible renders Uygun', () => {
      expect(renderEnablementStatus('ELIGIBLE_FOR_MANUAL_PREP')).toContain('Uygun')
    })
    it('blocked renders Engel', () => {
      expect(renderEnablementStatus('BLOCKED_BY_SAFETY')).toContain('Engel')
    })
    it('not eligible renders De\u011fil', () => {
      expect(renderEnablementStatus('NOT_ELIGIBLE')).toContain('De\u011fil')
    })
    it('review renders Gerekli', () => {
      expect(renderEnablementStatus('MANUAL_REVIEW_REQUIRED')).toContain('Gerekli')
    })
  })

  describe('B. Eligibility Check', () => {
    it('eligible true', () => expect(isEligibleForManualPrep(makeEligible())).toBe(true))
    it('blocked false', () => expect(isEligibleForManualPrep(makeBlocked())).toBe(false))
    it('not eligible false', () => expect(isEligibleForManualPrep(makeNotEligible())).toBe(false))
  })

  describe('C. Summary', () => {
    it('eligible summary shows T\u00fcrk\u00fc', () => {
      expect(getEnablementSummary(makeEligible())).toContain('\u00c7anakkale')
    })
    it('blocked summary shows engel', () => {
      expect(getEnablementSummary(makeBlocked())).toContain('engel')
    })
  })

  describe('D. Safety Prerequisites', () => {
    it('prerequisites rendered', () => {
      const s = getSafetyPrerequisites(makeEligible())
      expect(s).toContain('Telif')
    })
    it('checklist eligible all green', () => {
      const c = getPrerequisiteChecklist(makeEligible())
      expect(c).toContain('\u2705')
      expect(c).not.toContain('\u274c')
    })
    it('checklist not-eligible has red', () => {
      const c = getPrerequisiteChecklist(makeNotEligible())
      expect(c).toContain('\u274c')
    })
  })

  describe('E. Blocked Reasons', () => {
    it('eligible no blocks', () => {
      expect(getBlockedReasons(makeEligible())).toContain('yok')
    })
    it('blocked shows codes', () => {
      expect(getBlockedReasons(makeBlocked())).toContain('BLOCKED_SCENES_PRESENT')
    })
  })

  describe('F. Manual Steps', () => {
    it('eligible has steps', () => {
      expect(getManualSteps(makeEligible())).toContain('sa\u011flay\u0131c\u0131')
    })
    it('blocked has fix steps', () => {
      expect(getManualSteps(makeBlocked())).toContain('\u00e7\u00f6z\u00fcn')
    })
  })

  describe('G. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      const s = getEnablementSummary(makeEligible())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('steps no forbidden', () => {
      const s = getManualSteps(makeEligible())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/production/ManualEnablementPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('H. T\u00fcrk\u00e7e Characters', () => {
    it('status labels', () => {
      expect(renderEnablementStatus('FAILED_SAFE')).toContain('Ba\u015far\u0131s\u0131z')
    })
    it('checklist labels', () => {
      expect(getPrerequisiteChecklist(makeEligible())).toContain('Planl\u0131')
    })
  })
})
