/**
 * MP-003: Manual Production Enablement MVP Final Closure — frontend regression.
 */

import { describe, it, expect } from 'vitest'
import * as fs from 'fs'

const MP_COMPONENT_FILES = [
  'src/components/production/ManualEnablementPreview.tsx',
  'src/components/production/WorkOrderPreview.tsx',
]

const RP_COMPONENT_FILES = [
  'src/components/storyboard/StoryboardDraftPreview.tsx',
  'src/components/storyboard/SceneAssetPlanPreview.tsx',
  'src/components/storyboard/AnimaticTimelinePreview.tsx',
  'src/components/storyboard/MvpReadinessView.tsx',
]

const ALL_COMPONENT_FILES = [...RP_COMPONENT_FILES, ...MP_COMPONENT_FILES]

const FORBIDDEN_CONTROLS = [
  'DownloadButton', 'ExportButton', 'PublishButton', 'ApproveButton',
  'RenderButton', 'GenerateButton', 'ImageGenerator', 'ExecuteButton',
  'VideoPlayer', 'AudioPlayer', 'StreamPlayer',
  'innerHTML', 'dangerouslySetInnerHTML',
  'window.open', 'fetch(',
]

const FORBIDDEN_FIELDS = [
  'filePath', 'absolutePath', 'inputPath', 'outputPath',
  'stdout', 'stderr', 'commandLine', 'rawLog', 'shellCommand',
  'publicUrl', 'signedUrl', 'cdnUrl', 'downloadUrl', 'publishUrl', 'streamUrl',
  'bucketName', 'objectKey', 'storageKey', 'base64', 'binaryData', 'rawPayload',
  'token', 'JWT', 'session', 'principal', 'authorizationHeader',
  'lyrics', 'scriptBody', 'promptText', 'copyrightedContent',
  'providerPrompt', 'rawPrompt', 'modelInput', 'providerPayload',
]

// MP imports
import { renderEnablementStatus, isEligibleForManualPrep, getEnablementSummary, getManualSteps } from '../../production/ManualEnablementPreview'
import { renderWorkOrderStatus, isEligibleForManualPreparation, getWorkOrderSummary, renderChecklistItem, getWorkOrderSteps } from '../../production/WorkOrderPreview'
// RP imports
import { renderReadinessStatus, isReadyForManualReview, getReadinessSummary } from '../../storyboard/MvpReadinessView'

import type { TurkuManualEnablementResult, TurkuWorkOrderPreviewResult, TurkuProductionChecklistItem } from '../../../types'

function makeEnablementEligible(): TurkuManualEnablementResult {
  return {
    status: 'ELIGIBLE_FOR_MANUAL_PREP', blockedReasons: [],
    opaqueReadinessRef: 'req:a', safeTurkuTitleLabel: '\u00c7anakkale',
    operatorSummary: 'Manuel haz\u0131rl\u0131\u011fa uygun',
    safetyPrerequisites: ['Telif g\u00fcvenli'], manualStepLabels: ['Manuel onay'],
    copyrightSafeIntentPresent: true, allScenesPlanned: true,
    timelineComplete: true, noBlockedScenes: true,
  }
}

function makeChecklistItem(met: boolean): TurkuProductionChecklistItem {
  return { order: 1, label: 'Test', instruction: 'Manuel talimat', prerequisiteMet: met, blockedReason: met ? null : 'Engel' }
}

function makeWorkOrderEligible(): TurkuWorkOrderPreviewResult {
  return {
    status: 'ELIGIBLE_FOR_MANUAL_PREPARATION', blockedReasons: [],
    opaqueWorkOrderRef: 'wo:a', opaqueEnablementRef: 'en:a',
    safeTurkuTitleLabel: '\u00c7anakkale', operatorSummary: '6/6 tamamland\u0131',
    checklistItems: [makeChecklistItem(true)], totalItems: 1, completedItems: 1, blockedItems: 0,
    workOrderStepLabels: ['Kontrol listesini tamamlay\u0131n'],
  }
}

describe('MP-003 \u2014 MP Final Closure', () => {
  // A. Source File Safety Sweep (all 6 components)
  describe('A. Source File Safety Sweep (all RP+MP components)', () => {
    for (const file of ALL_COMPONENT_FILES) {
      it(`${file} has no forbidden controls`, () => {
        const src = fs.readFileSync(file, 'utf-8')
        for (const f of FORBIDDEN_CONTROLS) expect(src).not.toContain(f)
      })
    }
  })

  // B. Rendered Output Safety
  describe('B. Rendered Output Safety', () => {
    it('enablement summary no forbidden', () => {
      for (const f of FORBIDDEN_FIELDS) expect(getEnablementSummary(makeEnablementEligible())).not.toContain(f)
    })
    it('work order summary no forbidden', () => {
      for (const f of FORBIDDEN_FIELDS) expect(getWorkOrderSummary(makeWorkOrderEligible())).not.toContain(f)
    })
    it('checklist item no forbidden', () => {
      for (const f of FORBIDDEN_FIELDS) expect(renderChecklistItem(makeChecklistItem(true))).not.toContain(f)
    })
    it('manual steps no forbidden', () => {
      for (const f of FORBIDDEN_FIELDS) expect(getManualSteps(makeEnablementEligible())).not.toContain(f)
    })
    it('work order steps no forbidden', () => {
      for (const f of FORBIDDEN_FIELDS) expect(getWorkOrderSteps(makeWorkOrderEligible())).not.toContain(f)
    })
  })

  // C. Status Functions
  describe('C. Status Functions', () => {
    it('enablement eligible', () => expect(isEligibleForManualPrep(makeEnablementEligible())).toBe(true))
    it('work order eligible', () => expect(isEligibleForManualPreparation(makeWorkOrderEligible())).toBe(true))
    it('blocked enablement false', () => expect(isEligibleForManualPrep({ ...makeEnablementEligible(), status: 'BLOCKED_BY_SAFETY' })).toBe(false))
    it('blocked work order false', () => expect(isEligibleForManualPreparation({ ...makeWorkOrderEligible(), status: 'BLOCKED_BY_SAFETY' })).toBe(false))
  })

  // D. Status Labels
  describe('D. All Status Labels Renderable', () => {
    it('enablement statuses', () => {
      for (const s of ['NOT_ELIGIBLE', 'ELIGIBLE_FOR_MANUAL_PREP', 'BLOCKED_BY_SAFETY', 'MANUAL_REVIEW_REQUIRED', 'FAILED_SAFE'] as const)
        expect(renderEnablementStatus(s)).toBeTruthy()
    })
    it('work order statuses', () => {
      for (const s of ['DRAFT_PREVIEW', 'BLOCKED_BY_SAFETY', 'WAITING_FOR_MANUAL_REVIEW', 'ELIGIBLE_FOR_MANUAL_PREPARATION', 'FAILED_SAFE'] as const)
        expect(renderWorkOrderStatus(s)).toBeTruthy()
    })
  })

  // E. Type Safety
  describe('E. Type Safety', () => {
    it('enablement result no forbidden keys', () => {
      for (const f of ['publicUrl', 'filePath', 'token', 'lyrics', 'providerPrompt'])
        expect(Object.keys(makeEnablementEligible())).not.toContain(f)
    })
    it('work order result no forbidden keys', () => {
      for (const f of ['publicUrl', 'filePath', 'token', 'lyrics', 'providerPrompt'])
        expect(Object.keys(makeWorkOrderEligible())).not.toContain(f)
    })
  })

  // F. Checklist Rendering
  describe('F. Checklist Rendering', () => {
    it('met shows green', () => expect(renderChecklistItem(makeChecklistItem(true))).toContain('\u2705'))
    it('not met shows red', () => expect(renderChecklistItem(makeChecklistItem(false))).toContain('\u274c'))
    it('not met shows blocked', () => expect(renderChecklistItem(makeChecklistItem(false))).toContain('Engel'))
  })

  // G. T\u00fcrk\u00e7e
  describe('G. T\u00fcrk\u00e7e Characters', () => {
    it('enablement', () => expect(renderEnablementStatus('ELIGIBLE_FOR_MANUAL_PREP')).toContain('Uygun'))
    it('work order', () => expect(renderWorkOrderStatus('WAITING_FOR_MANUAL_REVIEW')).toContain('Bekleniyor'))
    it('readiness', () => expect(renderReadinessStatus('READY_FOR_MANUAL_REVIEW')).toContain('Haz\u0131r'))
  })
})
