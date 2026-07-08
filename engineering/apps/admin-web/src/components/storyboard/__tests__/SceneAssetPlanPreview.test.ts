/**
 * RP-003: Scene Asset Plan Preview UI Tests.
 */

import { describe, it, expect } from 'vitest'
import {
  renderAssetPlanStatus,
  isAssetPlanReady,
  renderAssetPlanCard,
  getAssetPlanSummary,
} from '../SceneAssetPlanPreview'
import type { TurkuSceneAssetPlanEntry, TurkuSceneAssetPlanResult } from '../../../types'

const FORBIDDEN_TEXT = [
  'filePath', 'absolutePath', 'inputPath', 'outputPath', 'workspacePath',
  'stdout', 'stderr', 'commandLine', 'rawLog', 'shellCommand',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey', 'storageKey', 'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal',
  'lyrics', 'scriptBody', 'promptText', 'copyrightedContent', 'userMedia',
  'providerPrompt', 'rawPrompt', 'modelInput',
]

const COMPONENT_FORBIDDEN = [
  'DownloadButton', 'ExportButton', 'PublishButton',
  'innerHTML', 'dangerouslySetInnerHTML',
  'ImageGenerator', 'img src=',
]

function makeSafeEntry(): TurkuSceneAssetPlanEntry {
  return {
    sceneNumber: 1, status: 'READY_FOR_ASSET_GENERATION_BOUNDARY', issueCodes: [],
    visualBriefSummary: 'Sahne 1: Giri\u015f \u2014 huzurlu, Anadolu tarz\u0131nda',
    shotType: 'WIDE', framing: 'Yatay manzara', atmosphere: 'S\u0131cak g\u00fcn \u0131\u015f\u0131\u011f\u0131',
    historicalCulturalStyle: 'Anadolu halk sanat\u0131', safetyClassification: 'SAFE', blockedReason: null,
  }
}

function makeBlockedEntry(): TurkuSceneAssetPlanEntry {
  return {
    ...makeSafeEntry(), status: 'BLOCKED_CONTENT_UNSAFE',
    issueCodes: ['SCENE_PURPOSE_EMPTY'], visualBriefSummary: null,
    shotType: null, framing: null, atmosphere: null, historicalCulturalStyle: null,
    safetyClassification: 'BLOCKED', blockedReason: 'Sahne amac\u0131 bo\u015f',
  }
}

function makeResult(): TurkuSceneAssetPlanResult {
  return {
    overallStatus: 'READY_FOR_ASSET_GENERATION_BOUNDARY', overallIssueCodes: [],
    opaquePlanRef: 'plan:abc', opaqueDraftRef: 'draft:xyz',
    safeTurkuTitleLabel: '\u00c7anakkale T\u00fcrk\u00fcs\u00fc', plannerMode: 'DETERMINISTIC_FAKE_TEST_DOUBLE',
    entries: [makeSafeEntry()], totalPlannedScenes: 1, blockedSceneCount: 0,
  }
}

describe('RP-003 \u2014 Scene Asset Plan Preview', () => {
  describe('A. Status Rendering', () => {
    it('ready renders Haz\u0131r', () => {
      expect(renderAssetPlanStatus('READY_FOR_ASSET_GENERATION_BOUNDARY')).toContain('Haz\u0131r')
    })
    it('blocked renders Engellendi', () => {
      expect(renderAssetPlanStatus('BLOCKED_CONTENT_UNSAFE')).toContain('Engellendi')
    })
    it('rejected renders Reddedildi', () => {
      expect(renderAssetPlanStatus('REJECTED_UNSAFE')).toContain('Reddedildi')
    })
  })

  describe('B. Ready Check', () => {
    it('ready returns true', () => expect(isAssetPlanReady(makeResult())).toBe(true))
    it('rejected returns false', () => {
      expect(isAssetPlanReady({ ...makeResult(), overallStatus: 'REJECTED_UNSAFE' })).toBe(false)
    })
  })

  describe('C. Card Rendering', () => {
    it('safe entry shows visual brief', () => {
      const c = renderAssetPlanCard(makeSafeEntry())
      expect(c).toContain('Sahne 1')
      expect(c).toContain('WIDE')
      expect(c).toContain('Anadolu')
    })
    it('blocked entry shows reason', () => {
      const c = renderAssetPlanCard(makeBlockedEntry())
      expect(c).toContain('Engel Nedeni')
      expect(c).toContain('bo\u015f')
    })
  })

  describe('D. Plan Summary', () => {
    it('summary shows metadata', () => {
      const s = getAssetPlanSummary(makeResult())
      expect(s).toContain('\u00c7anakkale')
      expect(s).toContain('DETERMINISTIC_FAKE_TEST_DOUBLE')
    })
  })

  describe('E. Forbidden Text Sweep', () => {
    it('card no forbidden', () => {
      const c = renderAssetPlanCard(makeSafeEntry())
      for (const f of FORBIDDEN_TEXT) expect(c).not.toContain(f)
    })
    it('summary no forbidden', () => {
      const s = getAssetPlanSummary(makeResult())
      for (const f of FORBIDDEN_TEXT) expect(s).not.toContain(f)
    })
  })

  describe('F. Source File Safety', () => {
    it('component no unsafe controls', async () => {
      const fs = await import('fs')
      const src = fs.readFileSync('src/components/storyboard/SceneAssetPlanPreview.tsx', 'utf-8')
      for (const f of COMPONENT_FORBIDDEN) expect(src).not.toContain(f)
    })
  })

  describe('G. Type Safety', () => {
    it('result no URL/path/prompt fields', () => {
      const keys = Object.keys(makeResult())
      for (const f of ['publicUrl', 'signedUrl', 'filePath', 'token', 'lyrics', 'providerPrompt']) {
        expect(keys).not.toContain(f)
      }
    })
  })

  describe('H. T\u00fcrk\u00e7e Characters', () => {
    it('status labels', () => {
      expect(renderAssetPlanStatus('VISUAL_BRIEF_GENERATED')).toContain('Olu\u015fturuldu')
    })
    it('card labels', () => {
      const c = renderAssetPlanCard(makeSafeEntry())
      expect(c).toContain('G\u00f6rsel \u00d6zet')
    })
  })
})
