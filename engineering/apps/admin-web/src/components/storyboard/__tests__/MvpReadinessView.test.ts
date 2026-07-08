/**
 * RP-005: MVP Readiness Package UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderReadinessStatus,
  isReadyForManualReview,
  getReadinessSummary,
  getSafetyChecklist,
  getBlockingIssues,
  getNextSteps,
} from '../MvpReadinessView'
import type { TurkuMvpReadinessResult } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'stdout', 'stderr', 'commandLine', 'rawLog',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey', 'storageKey', 'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal',
  'lyrics', 'scriptBody', 'promptText', 'providerPrompt',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'innerHTML', 'dangerouslySetInnerHTML',
  'ImageGenerator', 'RenderButton',
]

function makeReady(): TurkuMvpReadinessResult {
  return {
    status: 'READY_FOR_MANUAL_REVIEW', blockingIssues: [],
    opaqueRequestRef: 'sb-req:abc', safeTurkuTitleLabel: '\u00c7anakkale T\u00fcrk\u00fcs\u00fc',
    storyboardReadinessSummary: '4 sahne, DETERMINISTIC_FAKE_TEST_DOUBLE',
    assetPlanReadinessSummary: '4 planlanm\u0131\u015f, 0 engelli',
    timelineReadinessSummary: '4 \u00f6\u011fe, 40000ms toplam',
    totalScenes: 4, blockedScenes: 0, timelineItems: 4, totalDurationMs: 40000,
    copyrightSafeIntentPresent: true, allScenesPlanned: true,
    timelineComplete: true, noBlockedScenes: true,
    nextAllowedManualStepLabels: ['Manuel sa\u011flay\u0131c\u0131 yap\u0131land\u0131rmas\u0131', 'Manuel onay'],
  }
}

function makeNotReady(): TurkuMvpReadinessResult {
  return {
    ...makeReady(), status: 'NOT_READY',
    blockingIssues: ['STORYBOARD_MISSING', 'TIMELINE_MISSING'],
    storyboardReadinessSummary: 'Eksik', timelineReadinessSummary: 'Eksik',
    allScenesPlanned: false, timelineComplete: false,
    nextAllowedManualStepLabels: ['Eksik ad\u0131mlar\u0131 tamamlay\u0131n'],
  }
}

function makeBlocked(): TurkuMvpReadinessResult {
  return {
    ...makeReady(), status: 'BLOCKED_BY_SAFETY',
    blockingIssues: ['ASSET_PLAN_HAS_BLOCKED_SCENES'],
    blockedScenes: 1, noBlockedScenes: false,
    nextAllowedManualStepLabels: ['Engellenen sahneleri \u00e7\u00f6z\u00fcn'],
  }
}

describe('RP-005 \u2014 MVP Readiness Package', () => {
  describe('A. Status Rendering', () => {
    it('ready renders Haz\u0131r', () => {
      expect(renderReadinessStatus('READY_FOR_MANUAL_REVIEW')).toContain('Haz\u0131r')
    })
    it('not-ready renders De\u011fil', () => {
      expect(renderReadinessStatus('NOT_READY')).toContain('De\u011fil')
    })
    it('blocked renders Engel', () => {
      expect(renderReadinessStatus('BLOCKED_BY_SAFETY')).toContain('Engel')
    })
  })

  describe('B. Ready Check', () => {
    it('ready returns true', () => expect(isReadyForManualReview(makeReady())).toBe(true))
    it('not-ready returns false', () => expect(isReadyForManualReview(makeNotReady())).toBe(false))
    it('blocked returns false', () => expect(isReadyForManualReview(makeBlocked())).toBe(false))
  })

  describe('C. Summary', () => {
    it('ready summary shows chain data', () => {
      const s = getReadinessSummary(makeReady())
      expect(s).toContain('\u00c7anakkale')
      expect(s).toContain('sahne')
      expect(s).toContain('planlanm')
      expect(s).toContain('toplam')
    })
    it('not-ready shows Eksik', () => {
      const s = getReadinessSummary(makeNotReady())
      expect(s).toContain('Eksik')
    })
  })

  describe('D. Safety Checklist', () => {
    it('ready checklist all green', () => {
      const c = getSafetyChecklist(makeReady())
      expect(c).toContain('\u2705')
      expect(c).not.toContain('\u274c')
    })
    it('not-ready checklist has red', () => {
      const c = getSafetyChecklist(makeNotReady())
      expect(c).toContain('\u274c')
    })
  })

  describe('E. Blocking Issues', () => {
    it('ready no issues', () => {
      expect(getBlockingIssues(makeReady())).toContain('yok')
    })
    it('not-ready shows issues', () => {
      expect(getBlockingIssues(makeNotReady())).toContain('STORYBOARD_MISSING')
    })
  })

  describe('F. Next Steps', () => {
    it('ready has manual steps', () => {
      const s = getNextSteps(makeReady())
      expect(s).toContain('Manuel')
    })
    it('not-ready has fix steps', () => {
      expect(getNextSteps(makeNotReady())).toContain('tamamlay')
    })
  })

  describe('G. Forbidden Sweep', () => {
    it('summary no forbidden', () => {
      const s = getReadinessSummary(makeReady())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
    it('source no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/storyboard/MvpReadinessView.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('H. T\u00fcrk\u00e7e Characters', () => {
    it('status labels Turkish', () => {
      expect(renderReadinessStatus('PARTIALLY_READY')).toContain('Haz\u0131r')
    })
    it('checklist Turkish', () => {
      expect(getSafetyChecklist(makeReady())).toContain('Planl\u0131')
    })
  })
})
