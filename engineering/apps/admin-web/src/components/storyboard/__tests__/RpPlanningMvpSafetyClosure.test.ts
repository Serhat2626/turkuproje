/**
 * RP-006: RP Planning MVP Safety Closure — frontend regression tests.
 * Verifies all RP admin UI surfaces are safe.
 */

import { describe, it, expect } from 'vitest'
import * as fs from 'fs'

const RP_COMPONENT_FILES = [
  'src/components/storyboard/StoryboardDraftPreview.tsx',
  'src/components/storyboard/SceneAssetPlanPreview.tsx',
  'src/components/storyboard/AnimaticTimelinePreview.tsx',
  'src/components/storyboard/MvpReadinessView.tsx',
]

const FORBIDDEN_CONTROLS = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'GenerateButton', 'ImageGenerator',
  'VideoPlayer', 'AudioPlayer', 'StreamPlayer',
  'innerHTML', 'dangerouslySetInnerHTML',
  'window.open', 'fetch(',
]

const FORBIDDEN_FIELDS = [
  'filePath', 'absolutePath', 'inputPath', 'outputPath', 'workspacePath',
  'stdout', 'stderr', 'commandLine', 'rawLog', 'shellCommand',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey', 'storageKey', 'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal', 'authorizationHeader',
  'lyrics', 'scriptBody', 'promptText', 'copyrightedContent',
  'providerPrompt', 'rawPrompt', 'modelInput', 'providerPayload',
]

// Import all RP preview functions
import { renderDraftStatus, isDraftReady, renderSceneCard, getDraftSummary } from '../../storyboard/StoryboardDraftPreview'
import { renderAssetPlanStatus, isAssetPlanReady, renderAssetPlanCard, getAssetPlanSummary } from '../../storyboard/SceneAssetPlanPreview'
import { renderTimelineStatus, isTimelineReady, renderTimelineItem, getTimelineSummary } from '../../storyboard/AnimaticTimelinePreview'
import { renderReadinessStatus, isReadyForManualReview, getReadinessSummary, getSafetyChecklist, getBlockingIssues, getNextSteps } from '../../storyboard/MvpReadinessView'

import type {
  TurkuStoryboardDraftResult, TurkuStoryboardDraftScene,
  TurkuSceneAssetPlanEntry, TurkuSceneAssetPlanResult,
  TurkuAnimaticTimelineItem, TurkuAnimaticTimelinePlanResult,
  TurkuMvpReadinessResult,
} from '../../../types'

function makeDraftScene(): TurkuStoryboardDraftScene {
  return { sceneNumber: 1, purpose: 'Giri\u015f', mood: 'huzurlu', visualDirection: 'Anadolu ovalar\u0131', durationMs: 12000 }
}
function makeDraftResult(): TurkuStoryboardDraftResult {
  return {
    status: 'READY_FOR_ADMIN_PREVIEW', issueCodes: [], opaqueDraftRef: 'draft:a',
    opaqueRequestRef: 'req:a', safeTurkuTitleLabel: '\u00c7anakkale', copyrightSafeIntentSummary: 'telif g\u00fcvenli',
    orchestrationMode: 'DETERMINISTIC_FAKE_TEST_DOUBLE', scenes: [makeDraftScene()],
    totalSceneCount: 1, totalDurationMs: 12000, targetFormat: 'MP4',
    targetWidth: 1920, targetHeight: 1080, targetFps: 24,
  }
}
function makeAssetEntry(): TurkuSceneAssetPlanEntry {
  return {
    sceneNumber: 1, status: 'READY_FOR_ASSET_GENERATION_BOUNDARY', issueCodes: [],
    visualBriefSummary: 'Sahne 1: Giri\u015f', shotType: 'WIDE', framing: 'Yatay',
    atmosphere: 'S\u0131cak', historicalCulturalStyle: 'Anadolu', safetyClassification: 'SAFE', blockedReason: null,
  }
}
function makeAssetResult(): TurkuSceneAssetPlanResult {
  return {
    overallStatus: 'READY_FOR_ASSET_GENERATION_BOUNDARY', overallIssueCodes: [],
    opaquePlanRef: 'plan:a', opaqueDraftRef: 'draft:a', safeTurkuTitleLabel: '\u00c7anakkale',
    plannerMode: 'DETERMINISTIC_FAKE_TEST_DOUBLE', entries: [makeAssetEntry()],
    totalPlannedScenes: 1, blockedSceneCount: 0,
  }
}
function makeTimelineItem(): TurkuAnimaticTimelineItem {
  return {
    sceneNumber: 1, startMs: 0, durationMs: 10000, transitionType: 'FADE_IN',
    cameraMovement: 'STATIC', visualPacingSummary: 'Yava\u015f', narrativeBeatSummary: 'Tan\u0131t\u0131m',
  }
}
function makeTimelineResult(): TurkuAnimaticTimelinePlanResult {
  return {
    status: 'READY_FOR_ASSEMBLY_BOUNDARY', issueCodes: [],
    opaqueTimelineRef: 'tl:a', opaquePlanRef: 'plan:a', opaqueDraftRef: 'draft:a',
    safeTurkuTitleLabel: '\u00c7anakkale', plannerMode: 'DETERMINISTIC_FAKE_TEST_DOUBLE',
    items: [makeTimelineItem()], totalItems: 1, totalDurationMs: 10000,
  }
}
function makeReadinessReady(): TurkuMvpReadinessResult {
  return {
    status: 'READY_FOR_MANUAL_REVIEW', blockingIssues: [],
    opaqueRequestRef: 'req:a', safeTurkuTitleLabel: '\u00c7anakkale',
    storyboardReadinessSummary: '1 sahne', assetPlanReadinessSummary: '1 planlanm\u0131\u015f, 0 engelli',
    timelineReadinessSummary: '1 \u00f6\u011fe, 10000ms toplam',
    totalScenes: 1, blockedScenes: 0, timelineItems: 1, totalDurationMs: 10000,
    copyrightSafeIntentPresent: true, allScenesPlanned: true,
    timelineComplete: true, noBlockedScenes: true,
    nextAllowedManualStepLabels: ['Manuel onay'],
  }
}

describe('RP-006 \u2014 RP Planning MVP Safety Closure', () => {
  // A. Source File Safety Sweep
  describe('A. Source File Safety Sweep (all RP components)', () => {
    for (const file of RP_COMPONENT_FILES) {
      it(`${file} has no forbidden controls`, () => {
        const src = fs.readFileSync(file, 'utf-8')
        for (const f of FORBIDDEN_CONTROLS) expect(src).not.toContain(f)
      })
    }
  })

  // B. Rendered Output Safety
  describe('B. Rendered Output Safety', () => {
    it('draft summary no forbidden', () => {
      const s = getDraftSummary(makeDraftResult())
      for (const f of FORBIDDEN_FIELDS) expect(s).not.toContain(f)
    })
    it('asset card no forbidden', () => {
      const s = renderAssetPlanCard(makeAssetEntry())
      for (const f of FORBIDDEN_FIELDS) expect(s).not.toContain(f)
    })
    it('timeline item no forbidden', () => {
      const s = renderTimelineItem(makeTimelineItem())
      for (const f of FORBIDDEN_FIELDS) expect(s).not.toContain(f)
    })
    it('readiness summary no forbidden', () => {
      const s = getReadinessSummary(makeReadinessReady())
      for (const f of FORBIDDEN_FIELDS) expect(s).not.toContain(f)
    })
    it('safety checklist no forbidden', () => {
      const s = getSafetyChecklist(makeReadinessReady())
      for (const f of FORBIDDEN_FIELDS) expect(s).not.toContain(f)
    })
    it('next steps no forbidden', () => {
      const s = getNextSteps(makeReadinessReady())
      for (const f of FORBIDDEN_FIELDS) expect(s).not.toContain(f)
    })
  })

  // C. Ready/Status Functions
  describe('C. Ready/Status Functions', () => {
    it('isDraftReady', () => expect(isDraftReady(makeDraftResult())).toBe(true))
    it('isAssetPlanReady', () => expect(isAssetPlanReady(makeAssetResult())).toBe(true))
    it('isTimelineReady', () => expect(isTimelineReady(makeTimelineResult())).toBe(true))
    it('isReadyForManualReview', () => expect(isReadyForManualReview(makeReadinessReady())).toBe(true))
  })

  // D. Status Label Safety
  describe('D. Status Labels Safe', () => {
    it('all draft statuses renderable', () => {
      const statuses: Array<TurkuStoryboardDraftResult['status']> = [
        'CREATED_INTERNAL_ONLY', 'REQUEST_ACCEPTED', 'DRAFT_ORCHESTRATION_STARTED',
        'SCENES_GENERATED_SAFE', 'READY_FOR_ADMIN_PREVIEW', 'REJECTED_UNSAFE', 'FAILED_SAFE']
      for (const s of statuses) expect(renderDraftStatus(s)).toBeTruthy()
    })
    it('all asset plan statuses renderable', () => {
      const statuses: Array<TurkuSceneAssetPlanResult['overallStatus']> = [
        'CREATED_INTERNAL_ONLY', 'DRAFT_ACCEPTED', 'VISUAL_BRIEF_GENERATED',
        'READY_FOR_ASSET_GENERATION_BOUNDARY', 'BLOCKED_CONTENT_UNSAFE', 'REJECTED_UNSAFE', 'FAILED_SAFE']
      for (const s of statuses) expect(renderAssetPlanStatus(s)).toBeTruthy()
    })
    it('all timeline statuses renderable', () => {
      const statuses: Array<TurkuAnimaticTimelinePlanResult['status']> = [
        'CREATED_INTERNAL_ONLY', 'ASSET_PLAN_ACCEPTED', 'TIMELINE_ITEMS_GENERATED',
        'READY_FOR_ASSEMBLY_BOUNDARY', 'REJECTED_UNSAFE', 'FAILED_SAFE']
      for (const s of statuses) expect(renderTimelineStatus(s)).toBeTruthy()
    })
    it('all readiness statuses renderable', () => {
      const statuses: Array<TurkuMvpReadinessResult['status']> = [
        'NOT_READY', 'READY_FOR_MANUAL_REVIEW', 'BLOCKED_BY_SAFETY', 'PARTIALLY_READY', 'FAILED_SAFE']
      for (const s of statuses) expect(renderReadinessStatus(s)).toBeTruthy()
    })
  })

  // E. Type Safety
  describe('E. Type Safety (no forbidden keys)', () => {
    it('draft result', () => {
      for (const f of ['publicUrl', 'filePath', 'token', 'lyrics', 'providerPrompt'])
        expect(Object.keys(makeDraftResult())).not.toContain(f)
    })
    it('asset result', () => {
      for (const f of ['publicUrl', 'filePath', 'token', 'lyrics', 'providerPrompt'])
        expect(Object.keys(makeAssetResult())).not.toContain(f)
    })
    it('timeline result', () => {
      for (const f of ['publicUrl', 'filePath', 'token', 'lyrics', 'providerPrompt'])
        expect(Object.keys(makeTimelineResult())).not.toContain(f)
    })
    it('readiness result', () => {
      for (const f of ['publicUrl', 'filePath', 'token', 'lyrics', 'providerPrompt'])
        expect(Object.keys(makeReadinessReady())).not.toContain(f)
    })
  })

  // F. Blocking Issues
  describe('F. Blocking Issues Rendering', () => {
    it('no issues renders yok', () => {
      expect(getBlockingIssues(makeReadinessReady())).toContain('yok')
    })
    it('with issues renders codes', () => {
      const r = { ...makeReadinessReady(), status: 'NOT_READY' as const, blockingIssues: ['REQUEST_MISSING' as const] }
      expect(getBlockingIssues(r)).toContain('REQUEST_MISSING')
    })
  })

  // G. T\u00fcrk\u00e7e Characters
  describe('G. T\u00fcrk\u00e7e Characters Preserved', () => {
    it('draft', () => expect(renderDraftStatus('READY_FOR_ADMIN_PREVIEW')).toContain('Haz\u0131r'))
    it('asset', () => expect(renderAssetPlanStatus('VISUAL_BRIEF_GENERATED')).toContain('Olu\u015fturuldu'))
    it('timeline', () => expect(renderTimelineStatus('READY_FOR_ASSEMBLY_BOUNDARY')).toContain('Haz\u0131r'))
    it('readiness', () => expect(renderReadinessStatus('BLOCKED_BY_SAFETY')).toContain('Engel'))
  })
})
