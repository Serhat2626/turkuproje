import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

/**
 * FP-027 Task 03: Admin UI Queue/Retry/Expiration Visibility + Final Closure.
 */

const typesPath = path.resolve(__dirname, '../../types/index.ts')
const types = fs.readFileSync(typesPath, 'utf-8')
const pagePath = path.resolve(__dirname, '../TurkuVideoWorkflowPage.tsx')
const page = fs.readFileSync(pagePath, 'utf-8')
const apiPath = path.resolve(__dirname, '../../api/turkuVideoApi.ts')
const api = fs.readFileSync(apiPath, 'utf-8')

describe('FP-027 Task 03 — Admin UI Queue/Retry/Expiration + Closure', () => {

  // ── A. FP-027 Types ──
  describe('A. FP-027 Types', () => {
    it('TurkuInternalRenderQueueState', () => { expect(types).toContain('TurkuInternalRenderQueueState') })
    it('TurkuInternalRenderQueueAction', () => { expect(types).toContain('TurkuInternalRenderQueueAction') })
    it('TurkuInternalRenderQueueReasonCode', () => { expect(types).toContain('TurkuInternalRenderQueueReasonCode') })
    it('TurkuInternalRenderQueueSummary', () => { expect(types).toContain('TurkuInternalRenderQueueSummary') })
    it('TurkuInternalRenderQueueActionResult', () => { expect(types).toContain('TurkuInternalRenderQueueActionResult') })
    it('TurkuInternalRenderRetryPolicy', () => { expect(types).toContain('TurkuInternalRenderRetryPolicy') })
    it('TurkuInternalRenderRetrySnapshot', () => { expect(types).toContain('TurkuInternalRenderRetrySnapshot') })
    it('TurkuInternalRenderRetryReasonCode', () => { expect(types).toContain('TurkuInternalRenderRetryReasonCode') })
    it('TurkuInternalRenderExpirationPolicy', () => { expect(types).toContain('TurkuInternalRenderExpirationPolicy') })
    it('TurkuInternalRenderExpirationSnapshot', () => { expect(types).toContain('TurkuInternalRenderExpirationSnapshot') })
    it('TurkuInternalRenderExpirationReasonCode', () => { expect(types).toContain('TurkuInternalRenderExpirationReasonCode') })

    it('10 queue states', () => {
      ['NOT_QUEUED','QUEUE_READY','QUEUED_INTERNAL','DISPATCHING_INTERNAL','IN_PROGRESS_INTERNAL',
       'RETRY_WAIT_INTERNAL','SUCCEEDED_INTERNAL','FAILED_INTERNAL','CANCELLED_INTERNAL','EXPIRED_INTERNAL']
        .forEach(s => expect(types).toContain(`'${s}'`))
    })
    it('9 queue actions', () => {
      ['MARK_QUEUE_READY','ENQUEUE_INTERNAL','DISPATCH_INTERNAL','MARK_IN_PROGRESS','MARK_RETRY_WAIT',
       'MARK_SUCCEEDED','MARK_FAILED','CANCEL_INTERNAL','EXPIRE_INTERNAL']
        .forEach(a => expect(types).toContain(`'${a}'`))
    })
    it('7 retry reason codes', () => {
      ['EXECUTION_FAILED_RETRIABLE','TIMEOUT_RETRIABLE','TRANSIENT_ERROR_RETRIABLE',
       'MAX_RETRIES_EXHAUSTED','NOT_RETRIABLE_PERMANENT','CANCELLED_NO_RETRY','EXPIRED_NO_RETRY']
        .forEach(r => expect(types).toContain(`'${r}'`))
    })
    it('5 expiration reason codes', () => {
      ['QUEUE_DURATION_EXCEEDED','RETRY_WAIT_DURATION_EXCEEDED','STALE_INTERNAL_STATE',
       'ADMIN_FORCED_EXPIRATION','NOT_EXPIRED']
        .forEach(r => expect(types).toContain(`'${r}'`))
    })
  })

  // ── B. Queue Panel ──
  describe('B. Queue Panel', () => {
    it('section', () => { expect(page).toContain('fp027-queue-section') })
    it('status panel', () => { expect(page).toContain('fp027-queue-status-panel') })
    it('queue ref', () => { expect(page).toContain('fp027-queue-ref') })
    it('queue state', () => { expect(page).toContain('fp027-queue-state') })
    it('internal-only', () => { expect(page).toContain('fp027-internal-only') })
    it('public delivery', () => { expect(page).toContain('fp027-public-delivery') })
  })

  // ── C. Retry Panel ──
  describe('C. Retry Panel', () => {
    it('retry panel', () => { expect(page).toContain('fp027-retry-panel') })
    it('attempt count', () => { expect(page).toContain('fp027-attempt-count') })
    it('retry delay', () => { expect(page).toContain('fp027-retry-delay') })
    it('retriable', () => { expect(page).toContain('fp027-retriable') })
  })

  // ── D. Expiration Panel ──
  describe('D. Expiration Panel', () => {
    it('expiration panel', () => { expect(page).toContain('fp027-expiration-panel') })
    it('expiration status', () => { expect(page).toContain('fp027-expiration-status') })
    it('max queue duration', () => { expect(page).toContain('fp027-max-queue-duration') })
    it('max retry wait', () => { expect(page).toContain('fp027-max-retry-wait') })
  })

  // ── E. Dispatch Safety Panel ──
  describe('E. Dispatch Safety Panel', () => {
    it('panel', () => { expect(page).toContain('fp027-dispatch-safety-panel') })
    it('readiness required', () => { expect(page).toContain('fp027-readiness-required') })
    it('review required', () => { expect(page).toContain('fp027-review-required') })
    it('auto approval', () => { expect(page).toContain('fp027-auto-approval') })
    it('no publish', () => { expect(page).toContain('fp027-no-publish') })
  })

  // ── F. Forbidden Controls ──
  describe('F. Forbidden Controls', () => {
    it('no video player', () => { expect(page).not.toContain('<video'); expect(page).not.toContain('videoPlayer') })
    it('no download button', () => { expect(page).not.toContain('btn-download'); expect(page).not.toContain('downloadVideo') })
    it('no export button', () => { expect(page).not.toContain('btn-export'); expect(page).not.toContain('exportVideo') })
    it('no dangerouslySetInnerHTML', () => { expect(page).not.toContain('dangerouslySetInnerHTML') })
    it('no innerHTML', () => { expect(page).not.toContain('.innerHTML') })
    it('no Authorization/Bearer', () => { expect(page).not.toContain('Authorization'); expect(page).not.toContain('Bearer ') })
    it('no publicUrl/signedUrl/cdnUrl', () => { expect(page).not.toContain('publicUrl'); expect(page).not.toContain('signedUrl'); expect(page).not.toContain('cdnUrl') })
    it('no base64/binary', () => { expect(page).not.toContain('base64,'); expect(page).not.toContain('binaryData') })
    it('no generic status update', () => { expect(page).not.toContain('setStatus'); expect(page).not.toContain('updateStatus'); expect(page).not.toContain('patchStatus') })
  })

  // ── G. Forbidden Types Exposure ──
  describe('G. Forbidden Types Exposure', () => {
    const forbidden = [
      'filePath:', 'absolutePath:', 'storageKey:', 'publicUrl:', 'signedUrl:',
      'cdnUrl:', 'exportUrl:', 'publishUrl:', 'downloadUrl:', 'renderedVideoUrl:',
      'commandLine:', 'shellCommand:', 'executablePath:', 'workspacePath:',
      'inputPath:', 'outputPath:', 'rawRendererPayload:', 'rawProviderPayload:'
    ]
    const fp027Types = types.substring(types.indexOf('FP-027'))
    forbidden.forEach(f => {
      it(`FP-027 types '${f}' yok`, () => { expect(fp027Types).not.toContain(f) })
    })
  })

  // ── H. Turkish Safety Labels ──
  describe('H. Turkish Safety Labels', () => {
    it('dahili render kuyruğu', () => { expect(page).toContain('Dahili render kuyruğu') })
    it('retry bekleme güvenliği', () => { expect(page).toContain('Retry bekleme güvenliği') })
    it('süre aşımı politikası', () => { expect(page).toContain('Süre aşımı politikası') })
    it('dispatch insan onayı', () => { expect(page).toContain('Dispatch için insan onayı gerekli') })
    it('otomatik onay kapalı', () => { expect(page).toContain('Otomatik onay kapalı') })
    it('sadece dahili işleme', () => { expect(page).toContain('Sadece dahili işleme') })
    it('yayın yok', () => { expect(page).toContain('Yayın/Dışa aktarım yok') })
    it('indirme yok', () => { expect(page).toContain('İndirilebilir video yok') })
    it('player yok', () => { expect(page).toContain('Video oynatıcı yok') })
    it('url yok', () => { expect(page).toContain('Public/CDN URL yok') })
    it('path yok', () => { expect(page).toContain('Dosya yolu gösterilmez') })
    it('cmd yok', () => { expect(page).toContain('Komut/log gösterilmez') })
  })

  // ── I. API Client ──
  describe('I. API Client', () => {
    it('no publish', () => { expect(api).not.toContain('publishVideo') })
    it('no export', () => { expect(api).not.toContain('exportVideo') })
    it('no download', () => { expect(api).not.toContain('downloadVideo') })
    it('no autoApprove', () => { expect(api).not.toContain('autoApprove') })
    it('no generic status', () => { expect(api).not.toContain('setStatus'); expect(api).not.toContain('updateStatus') })
  })

  // ── J. Backward Compatibility ──
  describe('J. Backward Compatibility', () => {
    it('FP-024 types', () => { expect(types).toContain('TurkuRenderConfigMode'); expect(types).toContain('TurkuFfmpegExecutionMode') })
    it('FP-025 types', () => { expect(types).toContain('TurkuInternalRenderJobStatus'); expect(types).toContain('TurkuInternalRenderReadinessStatus') })
    it('FP-026 types', () => { expect(types).toContain('TurkuInternalRenderReviewState'); expect(types).toContain('TurkuInternalRenderReviewAction') })
    it('FP-019 types', () => { expect(types).toContain('TurkuVideoWorkflowStage') })
    it('FP-022 types', () => { expect(types).toContain('TurkuRenderJobStatus') })
  })
})
