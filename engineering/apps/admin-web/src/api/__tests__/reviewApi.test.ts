import { describe, it, expect } from 'vitest'
import * as reviewApi from '../reviewApi'

/**
 * FP-012 + FP-013 Task 04 + FP-014 Task 04 + FP-015 Task 04 + FP-016 Task 04:
 * Review API client tests.
 *
 * Kontrol noktaları:
 * - Sadece explicit action function'lar mevcut
 * - Generic status update function yok
 * - Publish/export/public URL/signed URL function yok
 * - Provider/storage inspection function yok
 * - FP-013 Task 04: handoffReview ve getPreview function'lar mevcut
 * - FP-014 Task 04: getInternalReadiness ve getApprovedInternalReadiness function'lar mevcut
 * - FP-015 Task 04: requestInternalRevision function mevcut
 * - FP-016 Task 04: getPipelineOverview ve getPipelineDetail function'lar mevcut
 */

describe('reviewApi', () => {
  // ── Exported Functions ──────────────────────────────────

  it('submitForReview fonksiyonu mevcut', () => {
    expect(typeof reviewApi.submitForReview).toBe('function')
  })

  it('startReview fonksiyonu mevcut', () => {
    expect(typeof reviewApi.startReview).toBe('function')
  })

  it('requestChanges fonksiyonu mevcut', () => {
    expect(typeof reviewApi.requestChanges).toBe('function')
  })

  it('approveForInternalUse fonksiyonu mevcut', () => {
    expect(typeof reviewApi.approveForInternalUse).toBe('function')
  })

  it('rejectReview fonksiyonu mevcut', () => {
    expect(typeof reviewApi.rejectReview).toBe('function')
  })

  it('returnToDraft fonksiyonu mevcut', () => {
    expect(typeof reviewApi.returnToDraft).toBe('function')
  })

  it('addReviewNote fonksiyonu mevcut', () => {
    expect(typeof reviewApi.addReviewNote).toBe('function')
  })

  it('getReviewState fonksiyonu mevcut', () => {
    expect(typeof reviewApi.getReviewState).toBe('function')
  })

  it('getAuditTimeline fonksiyonu mevcut', () => {
    expect(typeof reviewApi.getAuditTimeline).toBe('function')
  })

  it('handoffReview fonksiyonu mevcut', () => {
    expect(typeof reviewApi.handoffReview).toBe('function')
  })

  it('getPreview fonksiyonu mevcut', () => {
    expect(typeof reviewApi.getPreview).toBe('function')
  })

  it('getInternalReadiness fonksiyonu mevcut', () => {
    expect(typeof reviewApi.getInternalReadiness).toBe('function')
  })

  it('getApprovedInternalReadiness fonksiyonu mevcut', () => {
    expect(typeof reviewApi.getApprovedInternalReadiness).toBe('function')
  })

  it('requestInternalRevision fonksiyonu mevcut', () => {
    expect(typeof reviewApi.requestInternalRevision).toBe('function')
  })

  it('getPipelineOverview fonksiyonu mevcut', () => {
    expect(typeof reviewApi.getPipelineOverview).toBe('function')
  })

  it('getPipelineDetail fonksiyonu mevcut', () => {
    expect(typeof reviewApi.getPipelineDetail).toBe('function')
  })

  // ── Exact Export Count ──────────────────────────────────

  it('sadece 21 explicit function export ediyor', () => {
    const exportedFunctions = Object.keys(reviewApi).filter(
      k => typeof (reviewApi as Record<string, unknown>)[k] === 'function'
    )
    expect(exportedFunctions).toHaveLength(21)
    expect(exportedFunctions).toEqual(expect.arrayContaining([
      'submitForReview',
      'startReview',
      'requestChanges',
      'approveForInternalUse',
      'rejectReview',
      'returnToDraft',
      'addReviewNote',
      'getReviewState',
      'getAuditTimeline',
      'getWorklist',
      'getTargetContext',
      'getAvailableActions',
      'handoffReview',
      'getPreview',
      'getInternalReadiness',
      'getApprovedInternalReadiness',
      'requestInternalRevision',
      'getPipelineOverview',
      'getPipelineDetail',
      'getWorkflowObservabilitySummary',
      'getWorkflowObservabilityDiagnostics',
    ]))
  })

  // ── Forbidden Functions ─────────────────────────────────

  it('generic status update function yok', () => {
    const exports = Object.keys(reviewApi)
    const forbidden = [
      'updateStatus', 'setStatus', 'changeStatus',
      'updateReviewStatus', 'patchStatus', 'genericStatusUpdate',
    ]
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('publish/export function yok', () => {
    const exports = Object.keys(reviewApi)
    const forbidden = [
      'publish', 'export', 'publishReview', 'exportReview',
      'markCompleted', 'complete', 'readyForPublish',
      'markReadyForPublish', 'setReadyForPublish',
    ]
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('public/signed URL function yok', () => {
    const exports = Object.keys(reviewApi)
    const forbidden = [
      'getPublicUrl', 'getSignedUrl', 'createPublicUrl',
      'createSignedUrl', 'generatePublicUrl', 'generateSignedUrl',
    ]
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('provider/storage inspection function yok', () => {
    const exports = Object.keys(reviewApi)
    const forbidden = [
      'getProviderUrl', 'getStorageKey', 'inspectStorage',
      'inspectProvider', 'getConfig', 'inspectConfig',
    ]
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  // ── Forbidden Strings in Module ─────────────────────────

  it('module kaynak kodunda yasaklı davranış stringi yok', () => {
    const moduleSource = Object.keys(reviewApi).join(' ')
    const forbidden = [
      'READY_FOR_PUBLISH', 'COMPLETED', 'PUBLISHED', 'EXPORTED',
      'publicUrl', 'signedUrl', 'providerUrl', 'storageKey',
      'rawPayload', 'base64', 'secret', 'apiKey', 'stackTrace',
    ]
    forbidden.forEach(str => {
      expect(moduleSource).not.toContain(str)
    })
  })
})
