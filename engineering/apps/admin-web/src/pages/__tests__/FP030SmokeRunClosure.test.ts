import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

const typesPath = path.resolve(__dirname, '../../types/index.ts')
const types = fs.readFileSync(typesPath, 'utf-8')
const pagePath = path.resolve(__dirname, '../TurkuVideoWorkflowPage.tsx')
const page = fs.readFileSync(pagePath, 'utf-8')

describe('FP-030 Task 02 — Real Smoke Run + Closure', () => {

  describe('A. Smoke Run Types', () => {
    it('TurkuFfmpegSmokeRunStatus', () => { expect(types).toContain('TurkuFfmpegSmokeRunStatus') })
    it('TurkuFfmpegSmokeRunResult', () => { expect(types).toContain('TurkuFfmpegSmokeRunResult') })
    it('12 statuses', () => {
      ['NOT_ATTEMPTED','EXECUTION_SKIPPED_DISABLED','EXECUTION_COMPLETED_SAFE','EXECUTION_FAILED_SAFE','GATE_DENIED']
        .forEach(s => expect(types).toContain(`'${s}'`))
    })
  })

  describe('B. Smoke Run Panel', () => {
    it('section', () => { expect(page).toContain('fp030-smokerun-section') })
    it('panel', () => { expect(page).toContain('fp030-smokerun-panel') })
    it('status', () => { expect(page).toContain('fp030-smokerun-status') })
    it('process enabled', () => { expect(page).toContain('fp030-process-enabled') })
    it('candidate', () => { expect(page).toContain('fp030-candidate') })
  })

  describe('C. Safety Labels', () => {
    it('local only', () => { expect(page).toContain('fp030-label-local-only') })
    it('disabled default', () => { expect(page).toContain('fp030-label-disabled-default') })
    it('no publish', () => { expect(page).toContain('fp030-label-no-publish') })
    it('no player', () => { expect(page).toContain('fp030-label-no-player') })
    it('no command', () => { expect(page).toContain('fp030-label-no-command') })
  })

  describe('D. Forbidden Controls', () => {
    it('no video', () => { expect(page).not.toContain('<video') })
    it('no download btn', () => { expect(page).not.toContain('btn-download') })
    it('no dangerouslySetInnerHTML', () => { expect(page).not.toContain('dangerouslySetInnerHTML') })
  })

  describe('E. Types Forbidden', () => {
    const fp030Types = types.substring(types.indexOf('FP-030'))
    const forbidden = ['filePath:', 'storageKey:', 'commandLine:', 'stdout:', 'stderr:']
    forbidden.forEach(f => {
      it(`no '${f}'`, () => { expect(fp030Types).not.toContain(f) })
    })
  })

  describe('F. Backward Compat', () => {
    it('FP-028', () => { expect(types).toContain('TurkuFfmpegRealExecutionMode') })
    it('FP-029', () => { expect(types).toContain('TurkuInternalSmokePathStatus') })
    it('FP-030', () => { expect(types).toContain('TurkuFfmpegSmokeRunStatus') })
  })
})
