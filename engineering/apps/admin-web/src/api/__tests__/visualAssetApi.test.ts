import { describe, it, expect } from 'vitest'
import * as visualAssetApi from '../visualAssetApi'

/**
 * FP-006: Visual Asset Planning API client testleri.
 *
 * Kontrol noktaları:
 * - Sadece explicit action function'lar mevcut
 * - Generic status update function yok
 * - READY_FOR_PUBLISH function yok
 * - Publishing/media generation function yok
 * - Image generation/upload function yok
 * - Internal prompt/provider payload function yok
 */

describe('visualAssetApi', () => {
  // ── Exported Functions ──────────────────────────────────

  it('generateVisualAssetPlan fonksiyonu mevcut', () => {
    expect(typeof visualAssetApi.generateVisualAssetPlan).toBe('function')
  })

  it('listVisualAssetPlansByStoryboard fonksiyonu mevcut', () => {
    expect(typeof visualAssetApi.listVisualAssetPlansByStoryboard).toBe('function')
  })

  it('listVisualAssetPlansByContentProject fonksiyonu mevcut', () => {
    expect(typeof visualAssetApi.listVisualAssetPlansByContentProject).toBe('function')
  })

  it('getVisualAssetPlanDetail fonksiyonu mevcut', () => {
    expect(typeof visualAssetApi.getVisualAssetPlanDetail).toBe('function')
  })

  it('updateVisualAssetPlan fonksiyonu mevcut', () => {
    expect(typeof visualAssetApi.updateVisualAssetPlan).toBe('function')
  })

  it('updateSceneVisualPlan fonksiyonu mevcut', () => {
    expect(typeof visualAssetApi.updateSceneVisualPlan).toBe('function')
  })

  it('updateThumbnailPlan fonksiyonu mevcut', () => {
    expect(typeof visualAssetApi.updateThumbnailPlan).toBe('function')
  })

  it('approveVisualAssetPlan fonksiyonu mevcut', () => {
    expect(typeof visualAssetApi.approveVisualAssetPlan).toBe('function')
  })

  it('rejectVisualAssetPlan fonksiyonu mevcut', () => {
    expect(typeof visualAssetApi.rejectVisualAssetPlan).toBe('function')
  })

  // ── Function Count ──────────────────────────────────────

  it('sadece 9 explicit function export ediyor', () => {
    const exportedFunctions = Object.keys(visualAssetApi).filter(
      k => typeof (visualAssetApi as Record<string, unknown>)[k] === 'function'
    )
    expect(exportedFunctions).toHaveLength(9)
    expect(exportedFunctions).toEqual(expect.arrayContaining([
      'generateVisualAssetPlan',
      'listVisualAssetPlansByStoryboard',
      'listVisualAssetPlansByContentProject',
      'getVisualAssetPlanDetail',
      'updateVisualAssetPlan',
      'updateSceneVisualPlan',
      'updateThumbnailPlan',
      'approveVisualAssetPlan',
      'rejectVisualAssetPlan',
    ]))
  })

  // ── Governance ──────────────────────────────────────────

  it('generic status update function yok', () => {
    const exports = Object.keys(visualAssetApi)
    const forbidden = ['updateStatus', 'setStatus', 'changeStatus', 'patchStatus']
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('READY_FOR_PUBLISH function yok', () => {
    const exports = Object.keys(visualAssetApi)
    const forbidden = ['readyForPublish', 'markReadyForPublish', 'publishPlan', 'setReadyForPublish']
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('image generation/upload function yok', () => {
    const exports = Object.keys(visualAssetApi)
    const forbidden = [
      'generateImage', 'createImage', 'uploadImage', 'downloadImage',
      'generateMedia', 'uploadMedia', 'generateVideo', 'generateAudio',
    ]
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('publishing/scheduling function yok', () => {
    const exports = Object.keys(visualAssetApi)
    const forbidden = ['publish', 'schedule', 'upload', 'deploy']
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })

  it('internal prompt/provider function yok', () => {
    const exports = Object.keys(visualAssetApi)
    const forbidden = [
      'getRawPrompt', 'getProviderPayload', 'getProviderRequest',
      'getProviderResponse', 'getInternalPrompt',
    ]
    forbidden.forEach(fn => {
      expect(exports).not.toContain(fn)
    })
  })
})
