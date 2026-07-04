import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as visualAssetApi from '../visualAssetApi'

// ── Mock fetch to capture actual calls ────────────────────
const originalFetch = globalThis.fetch
let lastFetchUrl = ''
let lastFetchOptions: RequestInit = {}

beforeEach(() => {
  lastFetchUrl = ''
  lastFetchOptions = {}
  globalThis.fetch = vi.fn().mockResolvedValue({
    ok: true,
    json: () => Promise.resolve({ success: true, data: null, message: null, errors: null, timestamp: '' }),
  }) as unknown as typeof fetch
})

afterEach(() => {
  globalThis.fetch = originalFetch
})

function captureCall() {
  const call = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0]
  lastFetchUrl = call[0] as string
  lastFetchOptions = call[1] as RequestInit
}

import { afterEach } from 'vitest'

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
 * - Endpoint path doğruluğu
 * - HTTP method doğruluğu
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

  // ── Endpoint Path & HTTP Method ─────────────────────────

  describe('Endpoint doğruluğu', () => {
    it('generateVisualAssetPlan POST /admin/storyboard-drafts/{id}/visual-asset-plans/generate', async () => {
      await visualAssetApi.generateVisualAssetPlan('sb-123')
      captureCall()
      expect(lastFetchUrl).toContain('/admin/storyboard-drafts/sb-123/visual-asset-plans/generate')
      expect(lastFetchOptions.method).toBe('POST')
    })

    it('listVisualAssetPlansByStoryboard GET /admin/storyboard-drafts/{id}/visual-asset-plans', async () => {
      await visualAssetApi.listVisualAssetPlansByStoryboard('sb-123')
      captureCall()
      expect(lastFetchUrl).toContain('/admin/storyboard-drafts/sb-123/visual-asset-plans')
      expect(lastFetchUrl).not.toContain('generate')
    })

    it('listVisualAssetPlansByContentProject GET /admin/content-projects/{id}/visual-asset-plans', async () => {
      await visualAssetApi.listVisualAssetPlansByContentProject('proj-456')
      captureCall()
      expect(lastFetchUrl).toContain('/admin/content-projects/proj-456/visual-asset-plans')
    })

    it('getVisualAssetPlanDetail GET /admin/visual-asset-plans/{id}', async () => {
      await visualAssetApi.getVisualAssetPlanDetail('plan-789')
      captureCall()
      expect(lastFetchUrl).toContain('/admin/visual-asset-plans/plan-789')
      expect(lastFetchUrl).not.toContain('scenes')
      expect(lastFetchUrl).not.toContain('thumbnail')
    })

    it('updateVisualAssetPlan PATCH /admin/visual-asset-plans/{id}', async () => {
      await visualAssetApi.updateVisualAssetPlan('plan-789', { title: 'Test' })
      captureCall()
      expect(lastFetchUrl).toContain('/admin/visual-asset-plans/plan-789')
      expect(lastFetchOptions.method).toBe('PATCH')
      expect(lastFetchOptions.body).toContain('"title"')
    })

    it('updateSceneVisualPlan PATCH /admin/visual-asset-plans/{id}/scenes/{sceneNumber}', async () => {
      await visualAssetApi.updateSceneVisualPlan('plan-789', 3, { englishImagePrompt: 'test' })
      captureCall()
      expect(lastFetchUrl).toContain('/admin/visual-asset-plans/plan-789/scenes/3')
      expect(lastFetchOptions.method).toBe('PATCH')
      expect(lastFetchOptions.body).toContain('"englishImagePrompt"')
    })

    it('updateThumbnailPlan PATCH /admin/visual-asset-plans/{id}/thumbnail', async () => {
      await visualAssetApi.updateThumbnailPlan('plan-789', { thumbnailPrompt: 'test' })
      captureCall()
      expect(lastFetchUrl).toContain('/admin/visual-asset-plans/plan-789/thumbnail')
      expect(lastFetchOptions.method).toBe('PATCH')
      expect(lastFetchOptions.body).toContain('"thumbnailPrompt"')
    })

    it('approveVisualAssetPlan POST /admin/visual-asset-plans/{id}/approve', async () => {
      await visualAssetApi.approveVisualAssetPlan('plan-789')
      captureCall()
      expect(lastFetchUrl).toContain('/admin/visual-asset-plans/plan-789/approve')
      expect(lastFetchOptions.method).toBe('POST')
    })

    it('rejectVisualAssetPlan POST /admin/visual-asset-plans/{id}/reject with reason body', async () => {
      await visualAssetApi.rejectVisualAssetPlan('plan-789', { reason: 'Kalitesi yetersiz' })
      captureCall()
      expect(lastFetchUrl).toContain('/admin/visual-asset-plans/plan-789/reject')
      expect(lastFetchOptions.method).toBe('POST')
      expect(lastFetchOptions.body).toContain('"reason"')
      expect(lastFetchOptions.body).toContain('Kalitesi yetersiz')
    })
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
