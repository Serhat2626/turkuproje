import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

const typesPath = path.resolve(__dirname, '../../types/index.ts')
const types = fs.readFileSync(typesPath, 'utf-8')
const pagePath = path.resolve(__dirname, '../TurkuVideoWorkflowPage.tsx')
const page = fs.readFileSync(pagePath, 'utf-8')

describe('FP-029 Task 02 — Internal Generation Trial + Closure', () => {

  describe('A. Trial Types', () => {
    it('TurkuInternalTrialStatus', () => { expect(types).toContain('TurkuInternalTrialStatus') })
    it('TurkuInternalTrialReasonCode', () => { expect(types).toContain('TurkuInternalTrialReasonCode') })
    it('TurkuInternalTrialResult', () => { expect(types).toContain('TurkuInternalTrialResult') })
    it('9 trial statuses', () => {
      ['NOT_ATTEMPTED','GATE_DENIED','EXECUTION_SIMULATED','EXECUTION_ATTEMPTED_DEV_TEST','TRIAL_FAILED_SAFE']
        .forEach(s => expect(types).toContain(`'${s}'`))
    })
    it('16 trial reasons', () => {
      ['READINESS_NOT_MET','REVIEW_NOT_APPROVED','GATE_DENIED_MODE','PLAN_BOUNDS_INVALID','EXECUTION_SIMULATED_SAFE']
        .forEach(r => expect(types).toContain(`'${r}'`))
    })
  })

  describe('B. Trial Panel', () => {
    it('section', () => { expect(page).toContain('fp029-trial-section') })
    it('panel', () => { expect(page).toContain('fp029-trial-panel') })
    it('status', () => { expect(page).toContain('fp029-trial-status') })
    it('reason', () => { expect(page).toContain('fp029-trial-reason') })
    it('gate', () => { expect(page).toContain('fp029-trial-gate') })
    it('candidate', () => { expect(page).toContain('fp029-trial-candidate') })
    it('internal label', () => { expect(page).toContain('Sadece dahili deneme') })
    it('no publish', () => { expect(page).toContain('fp029-trial-no-publish') })
    it('no player', () => { expect(page).toContain('fp029-trial-no-player') })
    it('simulated label', () => { expect(page).toContain('fp029-trial-simulated') })
  })

  describe('C. Forbidden Controls', () => {
    it('no video', () => { expect(page).not.toContain('<video') })
    it('no download btn', () => { expect(page).not.toContain('btn-download') })
    it('no export btn', () => { expect(page).not.toContain('btn-export') })
    it('no dangerouslySetInnerHTML', () => { expect(page).not.toContain('dangerouslySetInnerHTML') })
    it('no publicUrl', () => { expect(page).not.toContain('publicUrl') })
  })

  describe('D. Trial Types Forbidden', () => {
    const trialTypes = types.substring(types.indexOf('FP-029 Task 02'))
    const forbidden = ['filePath:', 'storageKey:', 'publicUrl:', 'commandLine:', 'stdout:', 'stderr:']
    forbidden.forEach(f => {
      it(`no '${f}'`, () => { expect(trialTypes).not.toContain(f) })
    })
  })

  describe('E. Backward Compat', () => {
    it('FP-028', () => { expect(types).toContain('TurkuFfmpegRealExecutionMode') })
    it('FP-029 smoke', () => { expect(types).toContain('TurkuInternalSmokePathStatus') })
    it('FP-029 trial', () => { expect(types).toContain('TurkuInternalTrialStatus') })
  })
})
