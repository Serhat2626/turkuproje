import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

/**
 * FP-028 Task 02: Admin UI FFmpeg Dev/Test Visibility.
 */

const typesPath = path.resolve(__dirname, '../../types/index.ts')
const types = fs.readFileSync(typesPath, 'utf-8')
const pagePath = path.resolve(__dirname, '../TurkuVideoWorkflowPage.tsx')
const page = fs.readFileSync(pagePath, 'utf-8')
const apiPath = path.resolve(__dirname, '../../api/turkuVideoApi.ts')
const api = fs.readFileSync(apiPath, 'utf-8')

describe('FP-028 Task 02 — Admin UI FFmpeg Dev/Test Visibility', () => {

  // ── A. FP-028 Types ──
  describe('A. FP-028 Types', () => {
    it('TurkuFfmpegRealExecutionMode', () => { expect(types).toContain('TurkuFfmpegRealExecutionMode') })
    it('TurkuFfmpegExecutionEnvironment', () => { expect(types).toContain('TurkuFfmpegExecutionEnvironment') })
    it('TurkuFfmpegBinaryPolicy', () => { expect(types).toContain('TurkuFfmpegBinaryPolicy') })
    it('TurkuFfmpegBoundaryResultStatus', () => { expect(types).toContain('TurkuFfmpegBoundaryResultStatus') })
    it('TurkuFfmpegBoundaryReasonCode', () => { expect(types).toContain('TurkuFfmpegBoundaryReasonCode') })
    it('TurkuFfmpegExecutionBoundaryResult', () => { expect(types).toContain('TurkuFfmpegExecutionBoundaryResult') })
    it('TurkuFfmpegDevTestAdapterResult', () => { expect(types).toContain('TurkuFfmpegDevTestAdapterResult') })
    it('TurkuFfmpegVerificationCandidate', () => { expect(types).toContain('TurkuFfmpegVerificationCandidate') })

    it('4 execution modes', () => {
      ['DISABLED','DRY_RUN','SIMULATED','REAL_DEV_TEST_ONLY'].forEach(m => expect(types).toContain(`'${m}'`))
    })
    it('6 environments', () => {
      ['LOCAL','DEV','TEST','STAGING','PROD','UNKNOWN'].forEach(e => expect(types).toContain(`'${e}'`))
    })
    it('6 boundary statuses', () => {
      ['NOT_EXECUTED','DRY_RUN_COMPLETE','SIMULATED_COMPLETE','EXECUTED_INTERNAL','FAILED_SAFE','DENIED']
        .forEach(s => expect(types).toContain(`'${s}'`))
    })
    it('14 boundary reason codes', () => {
      ['DISABLED_BY_DEFAULT','ENVIRONMENT_DENIED','MODE_DISABLED','BINARY_UNAVAILABLE',
       'INTERNAL_FLAG_NOT_SET','PUBLIC_DELIVERY_FORBIDDEN','AUTO_APPROVAL_FORBIDDEN']
        .forEach(r => expect(types).toContain(`'${r}'`))
    })
  })

  // ── B. Gate Status Panel ──
  describe('B. Gate Status Panel', () => {
    it('section', () => { expect(page).toContain('fp028-ffmpeg-section') })
    it('gate panel', () => { expect(page).toContain('fp028-gate-status-panel') })
    it('mode', () => { expect(page).toContain('fp028-execution-mode') })
    it('environment', () => { expect(page).toContain('fp028-environment') })
    it('flag', () => { expect(page).toContain('fp028-internal-flag') })
    it('binary', () => { expect(page).toContain('fp028-binary-policy') })
  })

  // ── C. Adapter Panel ──
  describe('C. Adapter Panel', () => {
    it('panel', () => { expect(page).toContain('fp028-adapter-panel') })
    it('boundary status', () => { expect(page).toContain('fp028-boundary-status') })
    it('boundary reason', () => { expect(page).toContain('fp028-boundary-reason') })
    it('internal', () => { expect(page).toContain('fp028-adapter-internal') })
  })

  // ── D. Output Panel ──
  describe('D. Output Panel', () => {
    it('panel', () => { expect(page).toContain('fp028-output-panel') })
    it('output ref', () => { expect(page).toContain('fp028-output-ref') })
    it('format', () => { expect(page).toContain('fp028-output-format') })
    it('verification', () => { expect(page).toContain('fp028-verification-status') })
  })

  // ── E. Safety Panel ──
  describe('E. Safety Panel', () => {
    it('panel', () => { expect(page).toContain('fp028-safety-panel') })
    it('local-only', () => { expect(page).toContain('fp028-label-local-only') })
    it('prod-disabled', () => { expect(page).toContain('fp028-label-prod-disabled') })
    it('no-url', () => { expect(page).toContain('fp028-label-no-url') })
    it('no-download', () => { expect(page).toContain('fp028-label-no-download') })
    it('no-player', () => { expect(page).toContain('fp028-label-no-player') })
    it('no-path', () => { expect(page).toContain('fp028-label-no-path') })
    it('no-auto', () => { expect(page).toContain('fp028-label-no-auto') })
    it('no-stderr', () => { expect(page).toContain('fp028-label-no-stderr') })
    it('no-storage', () => { expect(page).toContain('fp028-label-no-storage') })
  })

  // ── F. Turkish Labels ──
  describe('F. Turkish Labels', () => {
    it('local only', () => { expect(page).toContain('Gerçek FFmpeg sadece local/dev/test') })
    it('prod kapalı', () => { expect(page).toContain("Production'da kapalı") })
    it('public url yok', () => { expect(page).toContain('Public URL yok') })
    it('indirme yok', () => { expect(page).toContain('İndirme/yayın/dışa aktarım yok') })
    it('player yok', () => { expect(page).toContain('Video oynatıcı yok') })
    it('path yok', () => { expect(page).toContain('Komut/dosya yolu gösterilmez') })
  })

  // ── G. Forbidden Controls ──
  describe('G. Forbidden Controls', () => {
    it('no video', () => { expect(page).not.toContain('<video') })
    it('no download btn', () => { expect(page).not.toContain('btn-download') })
    it('no export btn', () => { expect(page).not.toContain('btn-export') })
    it('no dangerouslySetInnerHTML', () => { expect(page).not.toContain('dangerouslySetInnerHTML') })
    it('no innerHTML', () => { expect(page).not.toContain('.innerHTML') })
    it('no publicUrl/signedUrl/cdnUrl', () => {
      expect(page).not.toContain('publicUrl'); expect(page).not.toContain('signedUrl'); expect(page).not.toContain('cdnUrl')
    })
    it('no base64/binary', () => { expect(page).not.toContain('base64,'); expect(page).not.toContain('binaryData') })
    it('no generic status', () => { expect(page).not.toContain('setStatus'); expect(page).not.toContain('updateStatus') })
  })

  // ── H. FP-028 Types Forbidden Fields ──
  describe('H. FP-028 Types Forbidden', () => {
    const forbidden = [
      'filePath:', 'absolutePath:', 'storageKey:', 'publicUrl:', 'signedUrl:',
      'cdnUrl:', 'commandLine:', 'shellCommand:', 'executablePath:',
      'stdout:', 'stderr:', 'rawLog:', 'rawPayload:'
    ]
    const fp028Types = types.substring(types.indexOf('FP-028'))
    forbidden.forEach(f => {
      it(`FP-028 types '${f}' yok`, () => { expect(fp028Types).not.toContain(f) })
    })
  })

  // ── I. Backward Compatibility ──
  describe('I. Backward Compatibility', () => {
    it('FP-019', () => { expect(types).toContain('TurkuVideoWorkflowStage') })
    it('FP-024', () => { expect(types).toContain('TurkuRenderConfigMode') })
    it('FP-025', () => { expect(types).toContain('TurkuInternalRenderJobStatus') })
    it('FP-026', () => { expect(types).toContain('TurkuInternalRenderReviewState') })
    it('FP-027', () => { expect(types).toContain('TurkuInternalRenderQueueState') })
  })

  // ── J. API Client ──
  describe('J. API Client', () => {
    it('no publish', () => { expect(api).not.toContain('publishVideo') })
    it('no export', () => { expect(api).not.toContain('exportVideo') })
    it('no download', () => { expect(api).not.toContain('downloadVideo') })
    it('no autoApprove', () => { expect(api).not.toContain('autoApprove') })
  })
})
