import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

const typesPath = path.resolve(__dirname, '../../types/index.ts')
const types = fs.readFileSync(typesPath, 'utf-8')
const pagePath = path.resolve(__dirname, '../TurkuVideoWorkflowPage.tsx')
const page = fs.readFileSync(pagePath, 'utf-8')

describe('FP-029 Task 01 — E2E Smoke Path Visibility', () => {

  describe('A. FP-029 Types', () => {
    it('TurkuInternalSmokePathStatus', () => { expect(types).toContain('TurkuInternalSmokePathStatus') })
    it('TurkuInternalSmokePathReasonCode', () => { expect(types).toContain('TurkuInternalSmokePathReasonCode') })
    it('TurkuInternalSmokeRenderPlan', () => { expect(types).toContain('TurkuInternalSmokeRenderPlan') })
    it('TurkuInternalSmokePathResult', () => { expect(types).toContain('TurkuInternalSmokePathResult') })
    it('10 statuses', () => {
      ['NOT_STARTED','CANDIDATE_CREATED','SMOKE_COMPLETE_INTERNAL','DENIED','FAILED_SAFE']
        .forEach(s => expect(types).toContain(`'${s}'`))
    })
    it('14 reasons', () => {
      ['READINESS_NOT_MET','REVIEW_NOT_APPROVED','GATE_DENIED','BOUNDS_INVALID','SMOKE_COMPLETE_SAFE']
        .forEach(r => expect(types).toContain(`'${r}'`))
    })
  })

  describe('B. Smoke Panels', () => {
    it('section', () => { expect(page).toContain('fp029-smoke-section') })
    it('status panel', () => { expect(page).toContain('fp029-smoke-status-panel') })
    it('status', () => { expect(page).toContain('fp029-smoke-status') })
    it('reason', () => { expect(page).toContain('fp029-smoke-reason') })
    it('readiness', () => { expect(page).toContain('fp029-readiness') })
    it('review', () => { expect(page).toContain('fp029-review') })
    it('gate', () => { expect(page).toContain('fp029-gate') })
    it('plan panel', () => { expect(page).toContain('fp029-plan-panel') })
    it('format', () => { expect(page).toContain('fp029-format') })
    it('output ref', () => { expect(page).toContain('fp029-output-ref') })
    it('verification', () => { expect(page).toContain('fp029-verification') })
  })

  describe('C. Safety Labels', () => {
    it('internal', () => { expect(page).toContain('Sadece dahili smoke test') })
    it('no publish', () => { expect(page).toContain('fp029-label-no-publish') })
    it('no download', () => { expect(page).toContain('fp029-label-no-download') })
    it('no player', () => { expect(page).toContain('fp029-label-no-player') })
    it('no public', () => { expect(page).toContain('fp029-label-no-public') })
    it('no auto', () => { expect(page).toContain('fp029-label-no-auto') })
  })

  describe('D. Forbidden Controls', () => {
    it('no video', () => { expect(page).not.toContain('<video') })
    it('no download btn', () => { expect(page).not.toContain('btn-download') })
    it('no dangerouslySetInnerHTML', () => { expect(page).not.toContain('dangerouslySetInnerHTML') })
    it('no publicUrl/signedUrl', () => { expect(page).not.toContain('publicUrl'); expect(page).not.toContain('signedUrl') })
  })

  describe('E. FP-029 Types Forbidden', () => {
    const fp029Types = types.substring(types.indexOf('FP-029'))
    const forbidden = ['filePath:', 'absolutePath:', 'storageKey:', 'publicUrl:', 'commandLine:', 'stdout:', 'stderr:']
    forbidden.forEach(f => {
      it(`no '${f}'`, () => { expect(fp029Types).not.toContain(f) })
    })
  })

  describe('F. Backward Compat', () => {
    it('FP-019', () => { expect(types).toContain('TurkuVideoWorkflowStage') })
    it('FP-025', () => { expect(types).toContain('TurkuInternalRenderJobStatus') })
    it('FP-026', () => { expect(types).toContain('TurkuInternalRenderReviewState') })
    it('FP-027', () => { expect(types).toContain('TurkuInternalRenderQueueState') })
    it('FP-028', () => { expect(types).toContain('TurkuFfmpegRealExecutionMode') })
  })
})
